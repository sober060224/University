package com.gdou.marinebio.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.marinebio.ai.DashScopeClient;
import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.AiRecord;
import com.gdou.marinebio.entity.AiType;
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

    // ========== 能力一：图像智能识别与物种鉴定 ==========

    public Map<String, Object> identify(AuthForms.Identify form, LoginUser user) {
        long start = System.currentTimeMillis();
        String raw = client.chatVision(IDENTIFY_PROMPT, form.imageUrl());
        JsonNode node = parseJson(raw);
        String chineseName = node.path("chineseName").asText("");
        double confidence = node.path("confidence").asDouble(0);

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
        String raw = client.chatText(TAXONOMY_PROMPT, "物种名称：" + form.name());
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

        String raw = client.chatText(TAG_PROMPT, context.toString());
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
    @Transactional
    public Map<String, Object> ask(AuthForms.Ask form, LoginUser user) {
        long start = System.currentTimeMillis();
        String question = form.question();

        // 用问题里的词去检索模块二（物种）与模块三（观测），作为回答依据
        String context = buildContext(question, user == null ? null : user.getRole());

        ChatMessage ask = new ChatMessage();
        ask.setUserId(user.getId());
        ask.setRole("user");
        ask.setContent(question);
        chatMessageRepository.save(ask);

        String answer = client.chatText(QA_PROMPT, "用户检索到的系统数据：\n" + context + "\n\n问题：" + question);

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
        String raw = client.chatText(TRANSLATE_PROMPT,
                "目标语言：" + form.targetLanguage() + "\n\n原文：\n" + form.text());
        saveRecord(user, AiType.TRANSLATE, form.text(), null, raw, null, null,
                client.textModel(), System.currentTimeMillis() - start, true);
        return Map.of("text", raw, "targetLanguage", form.targetLanguage());
    }

    // ========== 调用记录 ==========

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<AiRecord> records(Integer userId, org.springframework.data.domain.Pageable pageable) {
        return aiRecordRepository.findByUserId(userId, pageable);
    }

    private void saveRecord(LoginUser user, AiType type, String inputText, String inputImage,
                            String result, Double confidence, Integer targetSpeciesId,
                            String model, long costMs, boolean success) {
        try {
            AiRecord record = new AiRecord();
            record.setUserId(user == null ? null : user.getId());
            record.setType(type);
            record.setInputText(inputText);
            record.setInputImage(inputImage);
            record.setResult(result);
            record.setConfidence(confidence);
            record.setTargetSpeciesId(targetSpeciesId);
            record.setModel(model);
            record.setCostMs((int) costMs);
            record.setSuccess(success);
            aiRecordRepository.save(record);
        } catch (Exception e) {
            // 日志写失败不能影响主流程
            log.warn("写入 ai_records 日志失败：{}", e.getMessage());
        }
    }

    // ========== 工具方法 ==========

    private String buildContext(String question, Role role) {
        StringBuilder sb = new StringBuilder();
        var species = speciesService.search(role, question, null, null, null, null,
                org.springframework.data.domain.Pageable.ofSize(8)).getRecords();
        sb.append("【物种数据】\n");
        if (species.isEmpty()) {
            sb.append("（无匹配记录）\n");
        } else {
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
        var observations = observationService.mapPoints().stream().limit(10).toList();
        if (observations.isEmpty()) {
            sb.append("（无匹配记录）\n");
        } else {
            for (var o : observations) {
                sb.append("- ").append(nullToEmpty(o.get("observeTime"))).append(" 在 ")
                        .append(nullToEmpty(o.get("locationName")))
                        .append("（").append(o.get("ecosystem")).append("）观测到 ")
                        .append(o.get("speciesCount")).append(" 个物种\n");
            }
        }
        return sb.toString();
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
