package com.gdou.marinebio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.marinebio.ai.DashScopeClient;
import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.AiRecord;
import com.gdou.marinebio.entity.AiType;
import com.gdou.marinebio.entity.Ecosystem;
import com.gdou.marinebio.entity.Species;
import com.gdou.marinebio.repository.AiRecordRepository;
import com.gdou.marinebio.repository.ChatMessageRepository;
import com.gdou.marinebio.entity.ChatMessage;
import com.gdou.marinebio.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块五：智能服务。
 *
 * 每个能力都做三件事：拼提示词 → 调百炼 → 落一条 ai_records 日志
 * （非功能性需求要求「关键识别结果需记录日志，便于追溯与质量分析」）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 交给大模型做上下文的数据条数上限，同时也是各类检索的 size 上限 */
    private static final int CONTEXT_LIMIT = 8;

    /** 整段候选词的长度上限；超过这个长度的整段几乎不可能是物种名 */
    private static final int WHOLE_TOKEN_MAX = 12;
    /** 滑窗片段的最长长度：中文物种名与地点名的常见区间是 2~4 字 */
    private static final int FRAGMENT_MAX = 4;
    /** 候选词总数上限，避免一个长问题驱动任意多次数据库往返 */
    private static final int KEYWORD_LIMIT = 24;

    /**
     * ai_records.result 的写入上限。该列是 TEXT，上限 65535 <b>字节</b>而不是字符：
     * utf8mb4 下一个汉字最多占 3~4 字节，按字符截到 16000 才能保证放得下。
     * 请求里没有 max_tokens，模型一旦放飞就远超这个量，而超长 INSERT 会被拒、
     * saveRecord 的 catch 只留一行 warn —— 又变成「调用成功但审计日志少一行」。
     */
    private static final int RESULT_MAX = 16000;

    private static final String TAXONOMY_PROMPT = """
            你是一位海洋生物学领域的分类学专家。请根据用户给出的物种中文名或学名，补全该物种的分类信息。
            只输出一个 JSON 对象，不要输出任何解释文字，也不要使用 markdown 代码块包裹。字段如下：
            {"chineseName":"","scientificName":"","phylum":"门","className":"纲","orderName":"目","familyName":"科","genusName":"属","speciesName":"种","morphology":"形态特征，不超过200字","habits":"生活习性，不超过200字","protectionLevel":"保护等级，取值之一：国家一级/国家二级/国家重点保护/有重要生态、科学、社会价值/其他","endangerStatus":"濒危度，取值之一：未评估/无危/近危/易危/濒危/极危"}
            如果不能确定某个字段，请填写空字符串。
            """;

    private static final String IDENTIFY_PROMPT = """
            你是一位海洋生物图像识别专家。请识别图片中的海洋生物，并输出一个 JSON 对象，不要输出任何解释文字，也不要使用 markdown 代码块包裹。字段如下：
            {"chineseName":"识别出的中文名","scientificName":"学名，无法确定则空字符串","confidence":0到1之间的小数,"candidates":["候选物种1","候选物种2","候选物种3"],"description":"外观特征与识别依据，不超过150字"}
            confidence 表示你对本次识别的把握程度；把握不足 0.6 时请把可能的结果都放进 candidates。
            """;

    private static final String TRANSLATE_PROMPT = """
            请把下面这段海洋生物物种描述翻译成指定语言。只输出译文本身，不要加解释、前言或引号。
            """;

    private static final String TAG_PROMPT = """
            你是一位海洋生态观测数据审核专家。根据下面这次观测记录的环境与地点信息，生成自动标签并做异常检测。
            只输出一个 JSON 对象，不要输出任何解释文字，也不要使用 markdown 代码块包裹。字段如下：
            {"tags":["自动标签，最多3个，例如 繁殖期发现 / 高盐度环境 / 夜间观测"],"warnings":["异常提示，没有则空数组，例如 热带物种出现在温带海域"],"summary":"一句话概述，不超过60字"}
            """;

    private static final String QA_PROMPT = """
            你是广东海洋大学海洋生物多样性信息管理系统的科研助手。
            系统会给你用户的历史检索数据（JSON）。请只依据这些数据回答问题；数据不足时明确说明缺少哪些数据，不要编造物种名、观测地点或数字。
            回答用中文，简洁分点，控制在 300 字以内。
            """;

    private final DashScopeClient client;
    private final AiRecordRepository aiRecordRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SpeciesService speciesService;
    private final ObservationService observationService;
    /** 问答上下文要按「问题里点名的生态系统」定位观测记录，所以需要直接读生态系统 */
    private final EcosystemService ecosystemService;

    // ========== 能力一：图像智能识别与物种鉴定 ==========

    public Map<String, Object> identify(AuthForms.Identify form, LoginUser user) {
        long start = System.currentTimeMillis();
        String raw = callOrLog(user, AiType.IDENTIFY, null, form.imageUrl(), client.visionModel(), start,
                () -> client.chatVision(IDENTIFY_PROMPT, form.imageUrl()));
        JsonNode node = parseJson(raw);
        String chineseName = node.path("chineseName").asText("");
        // 提示词要求模型给 0~1 的小数，但模型并不总是照做：偶尔会直接写 85 表示 85%。
        // 原样存进 ai_records.confidence、原样发给前端，界面就会显示「8500%」，
        // 置信度的分档着色与阈值判断也全部失真。这里统一夹回 [0,1]。
        double confidence = Math.max(0.0, Math.min(1.0, node.path("confidence").asDouble(0)));

        // 把模型给出的名字回查本库已有物种记录，供用户一键关联（置信度低时给候选列表）
        List<Map<String, Object>> candidates = new ArrayList<>();
        for (String candidate : namesToTry(chineseName, node.path("candidates"))) {
            if (candidate.isBlank()) {
                continue;
            }
            speciesService.matchByName(candidate, user == null ? null : user.getRole()).forEach(s -> candidates.add(Map.of(
                    "id", s.getId(),
                    "chineseName", s.getChineseName(),
                    "scientificName", s.getScientificName() == null ? "" : s.getScientificName(),
                    "imageUrl", s.getImageUrl() == null ? "" : s.getImageUrl(),
                    "protectionLevel", s.getProtectionLevel() == null ? "" : s.getProtectionLevel())));
            if (candidates.size() >= 5) {
                break;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("chineseName", chineseName);
        result.put("scientificName", node.path("scientificName").asText(""));
        result.put("confidence", confidence);
        result.put("description", node.path("description").asText(""));
        result.put("candidates", candidates);
        result.put("raw", raw);

        saveRecord(user, AiType.IDENTIFY, null, form.imageUrl(), raw, confidence,
                candidates.isEmpty() ? null : (Integer) candidates.get(0).get("id"),
                client.visionModel(), System.currentTimeMillis() - start, true);
        return result;
    }

    // ========== 能力二：文本辅助分类与补全 ==========

    public Map<String, Object> complete(AuthForms.Complete form, LoginUser user) {
        long start = System.currentTimeMillis();
        String raw = callOrLog(user, AiType.COMPLETE, form.name(), null, client.textModel(), start,
                () -> client.chatText(TAXONOMY_PROMPT, "物种名称：" + form.name()));
        JsonNode node = parseJson(raw);

        Map<String, Object> result = new LinkedHashMap<>();
        for (String field : List.of("chineseName", "scientificName", "phylum", "className", "orderName",
                "familyName", "genusName", "speciesName", "morphology", "habits", "protectionLevel", "endangerStatus")) {
            result.put(field, node.path(field).asText(""));
        }
        result.put("raw", raw);

        saveRecord(user, AiType.COMPLETE, form.name(), null, raw, null, null,
                client.textModel(), System.currentTimeMillis() - start, true);
        return result;
    }

    // ========== 能力三：观测记录智能标签与异常检测（选做） ==========

        public Map<String, Object> analyze(AuthForms.Analyze form, LoginUser user) {
        long start = System.currentTimeMillis();

        // 异常检测要判断"该物种是否出现在这个海域"，必须拿物种库里已登记的分布信息
        // 交给大模型比对，否则模型只能凭空猜测。
        StringBuilder context = new StringBuilder();
        context.append("观测时间：").append(nullToEmpty(form.observeTime())).append('\n');
        context.append("观测地点：").append(nullToEmpty(form.locationName()))
                .append("，经度 ").append(form.longitude())
                .append("，纬度 ").append(form.latitude()).append('\n');
        context.append("生态系统：").append(nullToEmpty(form.ecosystemName())).append('\n');
        context.append("本次观测到的物种（括号内为该物种在系统物种库中登记的分布区域）：\n");
        for (String name : form.speciesNames() == null ? List.<String>of() : form.speciesNames()) {
            context.append("- ").append(name);
            speciesService.matchByName(name, user == null ? null : user.getRole()).stream().findFirst()
                    .ifPresent(s -> context.append("（").append(nullToEmpty(s.getDistribution())).append("）"));
            context.append('\n');
        }

        String raw = callOrLog(user, AiType.TAG, context.toString(), null, client.textModel(), start,
                () -> client.chatText(TAG_PROMPT, context.toString()));
        JsonNode node = parseJson(raw);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tags", toStringList(node.path("tags")));
        result.put("warnings", toStringList(node.path("warnings")));
        result.put("summary", node.path("summary").asText(""));
        result.put("raw", raw);

        saveRecord(user, AiType.TAG, context.toString(), null, raw, null,
                null, client.textModel(), System.currentTimeMillis() - start, true);
        return result;
    }
// ========== 能力四：智能问答与科研助手 ==========

    /**
     * 自然语言问答。愿景文档描述的是「把问题转换为结构化查询，再由大模型生成回答」，
     * 这里用关键词检索先把模块二/三的相关数据取出来作为上下文交给模型，
     * 效果足够且不引入 NL2SQL 那套复杂度。
     */
    public Map<String, Object> ask(AuthForms.Ask form, LoginUser user) {
        long start = System.currentTimeMillis();
        String question = form.question();

        // 用问题里的词去检索模块二（物种）与模块三（观测），作为回答依据
        String context = buildContext(question, user == null ? null : user.getRole());

        // 调大模型放在事务外：read-timeout 是 30 秒、还带 2 次重试，
        // 放在 @Transactional 里会占住一个连接池连接最长约 98 秒；
        // 连接池只有 10 个，10 个人同时提问就把整个应用拖死了（包括登录接口）。
        // 这里也不需要事务：chatText 只是纯 HTTP 调用，两个 save 各自自带事务。
        String answer = callOrLog(user, AiType.QA, question, null, client.textModel(), start,
                () -> client.chatText(QA_PROMPT, "用户检索到的系统数据：\n" + context + "\n\n问题：" + question));

        ChatMessage ask = new ChatMessage();
        ask.setUserId(user.getId());
        ask.setRole("user");
        ask.setContent(question);
        chatMessageRepository.save(ask);

        ChatMessage reply = new ChatMessage();
        reply.setUserId(user.getId());
        reply.setRole("assistant");
        reply.setContent(answer);
        chatMessageRepository.save(reply);

        saveRecord(user, AiType.QA, question, null, answer, null, null,
                client.textModel(), System.currentTimeMillis() - start, true);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("answer", answer);
        result.put("context", context);
        return result;
    }

    public List<ChatMessage> history(Integer userId) {
        return chatMessageRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Transactional
    public void clearHistory(Integer userId) {
        chatMessageRepository.deleteByUserId(userId);
    }

    // ========== 能力五：物种描述多语言支持 ==========

    public Map<String, Object> translate(AuthForms.Translate form, LoginUser user) {
        long start = System.currentTimeMillis();
        String raw = callOrLog(user, AiType.TRANSLATE, form.text(), null, client.textModel(), start,
                () -> client.chatText(TRANSLATE_PROMPT,
                        "目标语言：" + form.targetLanguage() + "\n\n原文：\n" + form.text()));
        saveRecord(user, AiType.TRANSLATE, form.text(), null, raw, null, null,
                client.textModel(), System.currentTimeMillis() - start, true);
        return Map.of("text", raw, "targetLanguage", form.targetLanguage());
    }

    // ========== 调用记录 ==========

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AiRecord> records(Integer userId, org.springframework.data.domain.Pageable pageable) {
        return aiRecordRepository.findByUserId(userId, pageable);
    }

    /**
     * 调大模型，失败时先落一条 success=false 的记录再把异常抛回去。
     *
     * <p>原来五个能力都是「调用成功才写日志」，且写进去的 success 一律是硬编码的 true。
     * 于是 ai_records 里永远只有成功行：超时、鉴权失败、上游 4xx 全部凭空消失，
     * success 这一列恒为真、不携带任何信息。而非功能性需求要求这张表「便于追溯与质量分析」——
     * 看不出失败率的日志无法做质量分析。失败原因存进 result，排查时能直接看到上游返回体。
     */
    private String callOrLog(LoginUser user, AiType type, String inputText, String inputImage,
                             String model, long start, java.util.function.Supplier<String> call) {
        try {
            return call.get();
        } catch (RuntimeException e) {
            saveRecord(user, type, inputText, inputImage, e.getMessage(), null, null, model,
                    System.currentTimeMillis() - start, false);
            throw e;
        }
    }

    private void saveRecord(LoginUser user, AiType type, String inputText, String inputImage,
                            String result, Double confidence, Integer targetSpeciesId,
                            String model, long costMs, boolean success) {
        try {
            AiRecord record = new AiRecord();
            record.setUserId(user == null ? null : user.getId());
            record.setType(type);
            // analyze 传给这里的是一整段拼接出来的观测上下文（时间、地点、经纬度、
            // 生态系统，外加最多 50 个物种名及其分布区域），长度远超 input_text 的
            // VARCHAR(1000)。超长的 INSERT 会被 MySQL 拒绝，而下面的 catch 只记一条
            // warn，于是「调用大模型成功但审计日志凭空少一行」——非功能性需求明确要求
            // 关键识别结果可追溯，所以这里按列宽截断，保证这一行一定落库。
            record.setInputText(cut(inputText, 1000));
            record.setInputImage(cut(inputImage, 255));
            record.setResult(cut(result, RESULT_MAX));
            record.setConfidence(confidence);
            record.setTargetSpeciesId(targetSpeciesId);
            record.setModel(cut(model, 50));
            record.setCostMs((int) Math.min(Math.max(costMs, Integer.MIN_VALUE), Integer.MAX_VALUE));
            record.setSuccess(success);
            aiRecordRepository.save(record);
        } catch (Exception e) {
            // 日志写失败不能影响主流程
            log.warn("写入 ai_records 日志失败：{}", e.getMessage());
        }
    }

    /** 按字符数截断，超长时保留前 max 个字符并留一个省略号标记 */
    private static String cut(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max - 1) + "…";
    }

    // ========== 工具方法 ==========

    /**
     * 从自然语言问题里挑出可用于「正向 like」的候选词。
     *
     * <p>这只剩兜底作用：问题里写了完整物种名时走 {@code findMentionedIn} 的反向匹配，
     * 那才是自然语言点名实体的正确方向。这里负责的是另一种情况——用户只写了片段
     * （「牡蛎」要能匹到「珍珠牡蛎」），此时必须把句子拆成足够短的候选词去 like 字段。
     *
     * <p>候选词有总数上限：每一次候选都会驱动一次数据库往返，不设上限等于让一个
     * 长问题任意放大查询数。
     */
    private List<String> keywordsOf(String question) {
        List<String> candidates = new ArrayList<>();
        if (question == null || question.isBlank()) {
            return candidates;
        }
        // 去掉「如何」「怎么样」「是什么」这类提问尾巴：它们不是物种名的一部分
        for (String token : question.split("[\\s,，。？?！!、；;：:（）()「」\"']+")) {
            String t = token.replaceAll("(如何|怎么样|怎样|是什么|多少|请问|介绍|查询|搜索|有哪些|变化趋势|情况)+$", "").trim();
            if (t.length() < 2) {
                continue;
            }
            // 整段先试一次：长度合理的整段最可能就是用户直接写的名字
            if (t.length() <= WHOLE_TOKEN_MAX && !candidates.contains(t)) {
                candidates.add(t);
            }
            // 再补 2~4 字的滑窗片段。中文物种名与地点名基本落在这个区间，
            // 「牡蛎」这类片段要能匹到「珍珠牡蛎」只能靠它。
            //
            // 原来这里写的是「长度超过 12 的整段直接丢弃」，长问题于是候选数为零，
            // 最后兜底把整句原样拿去 `字段 like %整句%`——字段只有 5 个字、问题有 20 个字，
            // 必然匹配不上。两个缺陷叠加，等于长问题恒定检索不到数据。
            for (int len = FRAGMENT_MAX; len >= 2; len--) {
                for (int i = 0; i + len <= t.length(); i++) {
                    if (candidates.size() >= KEYWORD_LIMIT) {
                        return candidates;
                    }
                    String gram = t.substring(i, i + len);
                    if (!candidates.contains(gram)) {
                        candidates.add(gram);
                    }
                }
            }
        }
        return candidates;
    }

    private String buildContext(String question, Role role) {
        StringBuilder sb = new StringBuilder();

        // 物种检索：先做反向实体链接（这句问题里写了哪个已登记物种的名字），
        // 没命中再退回正向关键词 like——用户只写「牡蛎」这种片段时还要能匹到「珍珠牡蛎」。
        //
        // 原来只有正向一条路：把整句问题当关键词去「字段 like %问题%」。字段只有 5 个字、
        // 问题有 20 个字，永远匹配不上；而 keywordsOf 又把超过 12 字的整句直接丢掉。
        // 于是 ChatView 界面上那三个示例问题一个都检索不到数据，模型只能对着
        // 「（无匹配记录）」作答，报告 5.17 承诺的「回答中的数据均可回溯到系统内的真实记录」
        // 落不了地。
        List<Species> species = speciesService.findMentionedIn(question, role, CONTEXT_LIMIT);
        String speciesHitBy = null;
        if (species.isEmpty()) {
            for (String kw : keywordsOf(question)) {
                species = speciesService.search(role, kw, null, null, null, null,
                        org.springframework.data.domain.Pageable.ofSize(CONTEXT_LIMIT)).getRecords();
                if (!species.isEmpty()) {
                    speciesHitBy = kw;
                    break;
                }
            }
        }
        sb.append("【物种数据】\n");
        if (species.isEmpty()) {
            sb.append("（无匹配记录）\n");
        } else {
            // 实测：问「中国鲎的分布情况如何」时库里没有鲎，滑窗片段「中国」却命中了「中国对虾」，
            // 上下文于是把风马牛不相及的物种当成答案依据交出去，模型只好半答半拒。
            // 停用词表治不了这个（「湛江」这类地名同样是 2 字片段，却确有必要留着），
            // 所以改为如实标注来源：把实际用到的检索片段写给模型，让它自己说明库中没有该物种。
            if (speciesHitBy != null) {
                sb.append("（注意：以下记录是用问题片段「").append(speciesHitBy)
                        .append("」模糊检索得到的，不代表用户点名的物种在本库中存在）\n");
            }
            for (var s : species) {
                sb.append("- ").append(s.getChineseName());
                if (s.getScientificName() != null) {
                    sb.append("（").append(s.getScientificName()).append("）");
                }
                sb.append("，门：").append(nullToEmpty(s.getPhylum()))
                        .append("，保护等级：").append(nullToEmpty(s.getProtectionLevel()))
                        .append("，濒危度：").append(nullToEmpty(s.getEndangerStatus()))
                        .append("，分布：").append(nullToEmpty(s.getDistribution()))
                        .append('\n');
            }
        }

        sb.append("\n【观测记录】\n");
        List<Map<String, Object>> observations = List.of();
        // 1) 问题点到了具体物种，就顺着关联关系取该物种的观测记录
        if (!species.isEmpty()) {
            observations = searchObservations(null, species.get(0).getId().longValue(), null, role);
        }
        // 2) 问题点到的是生态系统（「请总结红树林生态系统中物种数量的变化趋势」）。
        //    这句话里没有任何物种名，地点名又比问题长，只有按生态系统名反向匹配再过滤才落得下去。
        if (observations.isEmpty()) {
            Ecosystem ecosystem = ecosystemService.findMentionedIn(question);
            if (ecosystem != null) {
                observations = searchObservations(ecosystem.getId().longValue(), null, null, role);
            }
        }
        // 3) 前两条都没结果才退回按地点关键词检索
        String observationsHitBy = null;
        if (observations.isEmpty()) {
            for (String kw : keywordsOf(question)) {
                observations = searchObservations(null, null, kw, role);
                if (!observations.isEmpty()) {
                    observationsHitBy = kw;
                    break;
                }
            }
        }
        if (observations.isEmpty()) {
            sb.append("（无匹配记录）\n");
        } else {
            if (observationsHitBy != null) {
                sb.append("（注意：以下记录是用问题片段「").append(observationsHitBy)
                        .append("」模糊检索得到的，可能与用户问的地点无关）\n");
            }
            for (var o : observations) {
                sb.append("- ").append(nullToEmpty(o.get("observeTime"))).append(" 在 ")
                        .append(nullToEmpty(o.get("locationName")))
                        .append("（").append(o.get("ecosystem")).append("）观测到 ")
                        .append(o.get("speciesCount")).append(" 个物种\n");
            }
        }
        return sb.toString();
    }

    /** 观测记录的三个检索入口共用这一段映射，避免同一份拼装逻辑抄三遍 */
    private List<Map<String, Object>> searchObservations(Long ecosystemId, Long speciesId,
                                                         String keyword, Role role) {
        return observationService
                .search(role, ecosystemId, null, speciesId, blankToNull(keyword), null, null,
                        PageResult.of(0, CONTEXT_LIMIT, 100, null))
                .getRecords().stream().map(o -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("observeTime", o.getObserveTime());
                    item.put("locationName", o.getLocationName());
                    item.put("ecosystem", o.getEcosystem() == null ? "未指定" : o.getEcosystem().getName());
                    // 原来这里写死「若干」，交给模型的是一个占位词，模型只好把它编成一个数字。
                    // 计数走 listLinkedSpecies(role)：未公开物种对非特权角色会被遮蔽成占位条目，
                    // 数的是过滤后可见的条数，不会反过来泄露看不到的记录数。
                    item.put("speciesCount", observationService.listLinkedSpecies(o.getId(), role).size());
                    return item;
                }).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 优先用模型给的中文名，再用候选列表去回查本库。 */
    private List<String> namesToTry(String chineseName, JsonNode candidates) {
        List<String> names = new ArrayList<>();
        names.add(chineseName);
        candidates.forEach(node -> names.add(node.asText("")));
        return names;
    }

    private List<String> toStringList(JsonNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(node -> list.add(node.asText("")));
        return list;
    }

    private String nullToEmpty(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 大模型经常把 JSON 包在 ```json 代码块里，直接 JsonMapper 解析会失败，
     * 所以先剥掉围栏，再截取第一个完整的 JSON 对象。
     */
    private JsonNode parseJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BizException("大模型没有返回内容");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BizException("大模型返回的内容不是有效 JSON：" + raw);
        }
        try {
            return MAPPER.readTree(text.substring(start, end + 1));
        } catch (Exception e) {
            throw new BizException("解析大模型返回的 JSON 失败：" + e.getMessage());
        }
    }
}
