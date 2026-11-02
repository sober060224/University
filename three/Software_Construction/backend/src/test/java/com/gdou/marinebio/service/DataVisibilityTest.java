package com.gdou.marinebio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.marinebio.ai.DashScopeClient;
import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.AiRecord;
import com.gdou.marinebio.entity.Ecosystem;
import com.gdou.marinebio.entity.ObservationSpecies;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.Species;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.AiRecordRepository;
import com.gdou.marinebio.repository.ChatMessageRepository;
import com.gdou.marinebio.repository.EcosystemRepository;
import com.gdou.marinebio.repository.ObservationRepository;
import com.gdou.marinebio.repository.ObservationSpeciesRepository;
import com.gdou.marinebio.repository.SpeciesRepository;
import com.gdou.marinebio.repository.UserRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据可见性与几处「静默失败」修复的回归测试。
 *
 * <p>覆盖的对象都是同一类问题：编译不报错、接口也不返回错误，但数据悄悄漏了或悄悄丢了。
 * 这类缺陷在没有数据库的环境里最值得用单元测试锁住，因为跑不了集成测试就等于没测。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataVisibilityTest {

    @Mock
    private SpeciesRepository speciesRepository;
    @Mock
    private LogService logService;
    @Mock
    private ObservationRepository observationRepository;
    @Mock
    private EcosystemRepository ecosystemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DashScopeClient client;
    @Mock
    private AiRecordRepository aiRecordRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private SpeciesService speciesService;
    @Mock
    private ObservationService observationService;
    @Mock
    private EcosystemService ecosystemService;

    private static Species species(Integer id, String name, String phylum, boolean isPublic) {
        Species s = new Species();
        s.setId(id);
        s.setChineseName(name);
        s.setPhylum(phylum);
        s.setIsPublic(isPublic);
        return s;
    }

    private static LoginUser user(Role role) {
        User u = new User();
        u.setId(1);
        u.setUsername("u");
        u.setRealName("u");
        u.setPassword("h");
        u.setRole(role);
        u.setStatus(UserStatus.ACTIVE);
        u.setCredentialVersion(1);
        return new LoginUser(u);
    }

    // ===== 报告 表 3-2 第二层：数据行级过滤 =====

    @Test
    @DisplayName("门类下拉也必须按可见性过滤：原来只有列表和详情过滤，下拉漏了")
    void phylumDropdownIsFilteredByRole() {
        SpeciesService svc = new SpeciesService(speciesRepository, logService);

        svc.listPhylums(Role.PUBLIC);
        verify(speciesRepository).findDistinctPhylum(Boolean.TRUE);

        svc.listPhylums(Role.STUDENT);
        verify(speciesRepository, org.mockito.Mockito.times(2)).findDistinctPhylum(Boolean.TRUE);

        // 管理员与科研人员传 null 表示不加限制
        svc.listPhylums(Role.RESEARCHER);
        svc.listPhylums(Role.ADMIN);
        // verify 统计的是累计调用次数，所以这里一次性断言两次 null，而不是逐个角色各断一次
        verify(speciesRepository, org.mockito.Mockito.times(2)).findDistinctPhylum(null);
    }

    @Test
    @DisplayName("未登录（role=null）按最低权限处理，不能因为拿不到角色就放开全部")
    void anonymousFallsBackToPublicFilter() {
        SpeciesService svc = new SpeciesService(speciesRepository, logService);
        svc.listPhylums(null);
        verify(speciesRepository).findDistinctPhylum(Boolean.TRUE);

        when(speciesRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        svc.search(null, null, null, null, null, null, Pageable.unpaged());
        verify(speciesRepository).search(eq(Boolean.TRUE), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("大模型回连本库的候选集同样受 is_public 约束，否则未公开物种从识别接口漏出")
    void matchByNameRespectsVisibility() {
        SpeciesService svc = new SpeciesService(speciesRepository, logService);
        when(speciesRepository.findExactByName(anyBoolean(), any())).thenReturn(List.of());
        when(speciesRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        svc.matchByName("招潮蟹", Role.PUBLIC);
        verify(speciesRepository).findExactByName(eq(Boolean.TRUE), eq("招潮蟹"));
        svc.matchByName("招潮蟹", Role.ADMIN);
        verify(speciesRepository).findExactByName(isNull(), eq("招潮蟹"));
    }

    private static <T> T isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }

    // ===== 看板：用户与观测人统计不属于发给学生的内容 =====

    @Test
    @DisplayName("看板对用户相关统计做角色门控，学生的响应里根本没有这些键")
    void dashboardOmitsUserStatsForStudents() {
        when(speciesRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        when(speciesRepository.countVisible(any())).thenReturn(3L);
        when(observationRepository.count()).thenReturn(10L);
        when(ecosystemRepository.count()).thenReturn(6L);
        when(observationRepository.countByObserveTimeGreaterThanEqual(any())).thenReturn(0L);
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByRole(Role.RESEARCHER)).thenReturn(1L);
        when(userRepository.countByStatus(UserStatus.PENDING)).thenReturn(2L);
        when(speciesRepository.countGroupByPhylum(any())).thenReturn(List.<Object[]>of(new Object[]{"软体动物门", 3L}));
        when(speciesRepository.countGroupByProtectionLevel(any())).thenReturn(List.<Object[]>of(new Object[]{"无", 3L}));
        when(speciesRepository.countGroupByEndangerStatus(any())).thenReturn(List.<Object[]>of(new Object[]{"无危", 3L}));
        when(ecosystemRepository.statsWithCounts(anyInt())).thenReturn(List.of());
        when(observationRepository.countByMonth()).thenReturn(List.of());
        when(observationRepository.countByObserver())
                .thenReturn(List.<Object[]>of(new Object[]{"陈老师", 9L}));

        StatsService stats = new StatsService(observationRepository, ecosystemRepository,
                userRepository, speciesService(speciesRepository));

        Map<String, Object> student = stats.dashboard(Role.STUDENT);
        Map<String, Object> studentOverview = asMap(student.get("overview"));
        assertThat(studentOverview).doesNotContainKey("userTotal");
        assertThat(studentOverview).doesNotContainKey("pendingUserTotal");
        assertThat(studentOverview).doesNotContainKey("researcherTotal");
        assertThat(student).doesNotContainKey("observerStats");
        // 物种与观测本身仍然可见
        assertThat(studentOverview).containsKeys("speciesTotal", "observationTotal");

        Map<String, Object> admin = stats.dashboard(Role.ADMIN);
        assertThat(asMap(admin.get("overview"))).containsKeys("userTotal", "pendingUserTotal", "researcherTotal");
        assertThat(admin).containsKey("observerStats");
    }

    @Test
    @DisplayName("分布统计的空白标签收口：null 与空串都必须渲染成「未填写」，否则同一项出两根柱")
    void distributionStatsNormalizesBlankLabels() {
        // 表单允许清空保护等级/濒危度（存空串），而 countGroupByProtectionLevel/
        // countGroupByEndangerStatus 在 SQL 里只滤 is_public 不滤空值；
        // null 走「未填写」而空串原样下发 name:""，前端图表就会渲染出
        // 一根没有标签的柱和一根「未填写」的柱——同一概念拆成两根。
        when(speciesRepository.countGroupByPhylum(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"", 2L}, new Object[]{null, 1L}));
        when(speciesRepository.countGroupByProtectionLevel(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"  ", 4L}));
        when(speciesRepository.countGroupByEndangerStatus(any()))
                .thenReturn(List.<Object[]>of(new Object[]{"无危", 5L}));

        Map<String, List<Map<String, Object>>> stats =
                speciesService(speciesRepository).distributionStats(Role.ADMIN);

        assertThat(stats.get("phylum")).extracting(m -> m.get("name"))
                .containsOnly("未填写");
        assertThat(stats.get("protectionLevel")).extracting(m -> m.get("name"))
                .containsExactly("未填写");
        assertThat(stats.get("endangerStatus")).extracting(m -> m.get("name"))
                .containsExactly("无危");
    }

    private SpeciesService speciesService(SpeciesRepository repo) {
        return new SpeciesService(repo, logService);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    @Test
    @DisplayName("生态统计对公众只看已公开物种，避免从计数反推未公开数量")
    void ecosystemStatsPassTheVisibilityFlag() {
        when(ecosystemRepository.statsWithCounts(anyInt())).thenReturn(List.of());
        StatsService stats = new StatsService(observationRepository, ecosystemRepository,
                userRepository, speciesService(speciesRepository));

        stats.ecosystemStats(Role.PUBLIC);
        verify(ecosystemRepository).statsWithCounts(0);

        stats.ecosystemStats(Role.ADMIN);
        verify(ecosystemRepository).statsWithCounts(1);
    }

    // ===== 模块五审计日志：超长不能静默丢行 =====

    @Test
    @DisplayName("analyze 的上下文远超 input_text 列宽，必须截断后落库而不是丢整条记录")
    void analyzeContextIsTruncatedIntoAuditLog() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        // 50 个物种名，每个 150 字，再叠上分布区域：拼出来的 context 有数万字符
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            names.add("种".repeat(150) + i);
        }
        when(client.chatText(any(), any())).thenReturn("{\"tags\":[\"夜间观测\"],\"warnings\":[],\"summary\":\"ok\"}");
        when(client.textModel()).thenReturn("qwen-plus");
        when(speciesService.matchByName(any(), any())).thenReturn(
                List.of(species(1, "招潮蟹", "节肢动物门", true)));

        ai.analyze(new AuthForms.Analyze("2026-05-01 09:30", "雷州湾", 110.4, 21.5, "红树林生态系统", names),
                user(Role.RESEARCHER));

        ArgumentCaptor<AiRecord> captor = ArgumentCaptor.forClass(AiRecord.class);
        verify(aiRecordRepository).save(captor.capture());
        AiRecord saved = captor.getValue();

        // VARCHAR(1000)：不截断就会被 MySQL 拒掉，而 saveRecord 的 catch 只记一条 warn，
        // 结果是「调用成功但审计表里凭空少一行」，直接违反非功能性需求里的可追溯要求。
        assertThat(saved.getInputText()).isNotNull().hasSizeLessThanOrEqualTo(1000);
        assertThat(saved.getType().name()).isEqualTo("TAG");
        assertThat(saved.getSuccess()).isTrue();
    }

    @Test
    @DisplayName("大模型把置信度写成 85 时夹回 [0,1]，不能让前端显示 8500%")
    void confidenceIsClampedIntoUnitRange() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        when(client.chatVision(any(), any())).thenReturn(
                "{\"chineseName\":\"招潮蟹\",\"scientificName\":\"Tubuca\",\"confidence\":85,\"candidates\":[],\"description\":\"x\"}");
        when(client.visionModel()).thenReturn("qwen-vl-max");
        when(speciesService.matchByName(any(), any())).thenReturn(List.of());

        Map<String, Object> result = ai.identify(new AuthForms.Identify("/uploads/2026/10/a.jpg"), user(Role.PUBLIC));

        assertThat((Double) result.get("confidence")).isBetween(0.0, 1.0);

        ArgumentCaptor<AiRecord> captor = ArgumentCaptor.forClass(AiRecord.class);
        verify(aiRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getConfidence()).isBetween(0.0, 1.0);
        assertThat(captor.getValue().getInputImage()).isEqualTo("/uploads/2026/10/a.jpg");
    }

    // ===== 请求体校验：坐标与环境参数不能什么都能填 =====

    @Test
    @DisplayName("经纬度、水温、盐度、水深有取值范围，超长文本字段有长度上限")
    void formBoundsAreEnforced() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        SpeciesForms.Create ok = new SpeciesForms.Create(
                "招潮蟹", "Tubuca spp.", "节肢动物门", "软甲纲", "十足目", "沙蟹科", "招潮蟹属", "弧边招潮蟹",
                "形态", "习性", "华南沿海", 110.41, 21.59, "国家二级", "易危", "/uploads/a.jpg",
                "https://example.com/v", "文献", true);
        assertThat(validator.validate(ok)).isEmpty();

        // 1e300 这类值能顺利写进库，但 Leaflet 拿到之后整张地图的缩放范围会被带飞
        SpeciesForms.Create badLon = new SpeciesForms.Create(
                "招潮蟹", null, null, null, null, null, null, null, null, null, null,
                1000.0, null, null, null, null, null, null, null);
        assertThat(validator.validate(badLon)).anySatisfy(v ->
                assertThat(v.getMessage()).contains("经度"));

        SpeciesForms.Create badLat = new SpeciesForms.Create(
                "招潮蟹", null, null, null, null, null, null, null, null, null, null,
                null, -91.0, null, null, null, null, null, null);
        assertThat(validator.validate(badLat)).anySatisfy(v ->
                assertThat(v.getMessage()).contains("纬度"));

        SpeciesForms.Create tooLong = new SpeciesForms.Create(
                "名".repeat(101), null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        assertThat(validator.validate(tooLong)).isNotEmpty();

        SpeciesForms.Observation bigDepth = new SpeciesForms.Observation(
                1, java.time.LocalDateTime.now(), null, null, null, null, null, 999999.0, null, null,
                List.of(new SpeciesForms.SpeciesLink(1, 1, null, null)));
        assertThat(validator.validate(bigDepth)).anySatisfy(v ->
                assertThat(v.getMessage()).contains("水深"));

        // TEXT 列以前没有上限：超长会一路走到 MySQL 才报 Data too long，
        // 用户看到的是「服务暂时不可用」这种误导性说法
        SpeciesForms.Create hugeMorphology = new SpeciesForms.Create(
                "招潮蟹", null, null, null, null, null, null, null, "x".repeat(2001), null, null,
                null, null, null, null, null, null, null, null);
        assertThat(validator.validate(hugeMorphology)).anySatisfy(v ->
                assertThat(v.getMessage()).contains("形态特征"));
    }

    @Test
    @DisplayName("登录字段限长：这是少数 permitAll 接口之一，不能接收任意大的请求体")
    void loginFieldsAreBounded() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new AuthForms.Login("admin", "admin123"))).isEmpty();
        assertThat(validator.validate(new AuthForms.Login("u".repeat(51), "pw"))).isNotEmpty();
        assertThat(validator.validate(new AuthForms.Login("admin", "p".repeat(101)))).isNotEmpty();
        assertThat(validator.validate(new AuthForms.Login("", "pw"))).isNotEmpty();
    }

    @Test
    @DisplayName("每条长度校验都要写明中文字段名，不能落回「个数必须在0和N之间」这种不指名字段的框架默认值")
    void everyValidationMessageIsChinese() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        // 每个字段都给一个「一定超长」的值，把这一层里所有约束的默认消息全部触发出来
        SpeciesForms.Create oversized = new SpeciesForms.Create(
                n(101), n(151), n(31), n(31), n(31), n(31), n(31), n(31),
                n(2001), n(2001), n(256), null, null, n(31), n(31), n(256), n(256), n(2001), true);

        SpeciesForms.Observation obs = new SpeciesForms.Observation(
                1, java.time.LocalDateTime.now(), null, null, n(201), null, null, null, n(31), n(2001),
                List.of(new SpeciesForms.SpeciesLink(null, -1, n(256), n(501))));

        SpeciesForms.Ecosystem eco = new SpeciesForms.Ecosystem(n(101), n(31), n(2001), n(256));

        AuthForms.Register reg = new AuthForms.Register(n(51), n(51), n(51), "STUDENT", n(21), n(101), n(31));
        AuthForms.Profile profile = new AuthForms.Profile(n(51), n(11), n(21), n(101), n(256), n(31));
        AuthForms.Approve approve = new AuthForms.Approve(true, n(21));
        AuthForms.Ask ask = new AuthForms.Ask(n(1001));

        List<jakarta.validation.ConstraintViolation<?>> violations = new ArrayList<>();
        violations.addAll(validator.validate(oversized));
        violations.addAll(validator.validate(obs));
        violations.addAll(validator.validate(eco));
        violations.addAll(validator.validate(reg));
        violations.addAll(validator.validate(profile));
        violations.addAll(validator.validate(approve));
        violations.addAll(validator.validate(ask));

        // 先确认这些表单真的产生了违约，否则下面的断言会因为「一条都没有」而空过
        assertThat(violations).as("至少要触发一批校验失败").isNotEmpty();

        // 框架自带的中文默认值形如「个数必须在0和30之间」：既点不出是哪个字段，
        // 「个数」用来描述字符串长度也是错的。而 GlobalExceptionHandler 会把同一张表单的
        // 所有消息用「；」拼成一句返回，于是超限三个字段就是
        // 「个数必须在0和30之间；个数必须在0和255之间；个数必须在0和100之间」。
        List<String> generic = violations.stream()
                .map(v -> v.getPropertyPath() + " -> " + v.getMessage())
                .filter(text -> text.contains("个数必须在") || text.contains("大小必须在"))
                .toList();

        assertThat(generic)
                .as("以下约束没有写中文 message，用户只会看到不指名字段的框架默认提示")
                .isEmpty();
    }

    // ===== 智能问答的实体链接 =====

    @Test
    @DisplayName("问题里写了完整物种名时走反向链接，并把该物种的观测记录一并带上")
    void questionNamingASpeciesLinksToThatSpecies() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        Species crab = species(9, "中华绒螯蟹", "节肢动物门", true);
        crab.setId(9);
        crab.setEndangerStatus("无危");
        crab.setProtectionLevel("无");
        crab.setDistribution("中国沿海河口与淡水水域");
        when(client.textModel()).thenReturn("qwen-plus");
        when(speciesService.findMentionedIn(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(crab));
        when(observationService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 8));
        // 反向链接命中时不该走正向兜底，但既然它是同一条链路上的另一支，这里也给个空结果：
        // 万一有人把反向链接删掉，测试会以「断言失败」而不是 NullPointerException 的形式报告。
        when(speciesService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 8));
        when(client.chatText(any(), any())).thenReturn("根据系统数据……");

        ai.ask(new AuthForms.Ask("请查一下中华绒螯蟹的分布区域"), user(Role.STUDENT));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).chatText(any(), prompt.capture());
        // 交给模型的上下文必须真的带上这个物种，而不是「（无匹配记录）」
        assertThat(prompt.getValue()).contains("中华绒螯蟹");
        assertThat(prompt.getValue()).doesNotContain("【物种数据】\n（无匹配记录）");

        // 观测记录要顺着命中的物种去取，不能再按整句问题瞎搜；
        // 角色也必须一路传到 observationService.search，否则物种维度的可见性会在这一环丢掉。
        verify(observationService).search(eq(Role.STUDENT),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), eq(9L),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    @DisplayName("界面示例问题不再被当成 LIKE 模式：整句最长 20 字，字段只有几字，必然匹配不上")
    void exampleQuestionsProduceUsableCandidates() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        when(client.textModel()).thenReturn("qwen-plus");
        when(speciesService.findMentionedIn(any(), any(), anyInt())).thenReturn(List.of());
        when(ecosystemService.findMentionedIn(any())).thenReturn(null);
        when(speciesService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 8));
        when(observationService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 8));
        when(client.chatText(any(), any())).thenReturn("数据不足");

        // ChatView 界面上三个示例问题中的第一个
        ai.ask(new AuthForms.Ask("最近三年在湛江附近观测到的濒危物种有哪些？"), user(Role.STUDENT));

        ArgumentCaptor<String> keyword = ArgumentCaptor.forClass(String.class);
        verify(speciesService, org.mockito.Mockito.atLeastOnce())
                .search(any(), keyword.capture(), any(), any(), any(), any(), any());

        // 原来这里只会收到整句原文（或干脆一次都不调用），像 %整句% 永远查不到东西
        assertThat(keyword.getAllValues())
                .as("候选检索词必须是足够短的片段")
                .allSatisfy(k -> assertThat(k.length()).isLessThanOrEqualTo(12));
        assertThat(keyword.getAllValues()).noneMatch(k -> k.contains("？"));
    }

    @Test
    @DisplayName("一句话里同时命中长短两个名字时，更具体的那条胜出（判据在 Java 侧，不依赖 SQL 函数排序）")
    void mostSpecificNameWins() {
        SpeciesService svc = new SpeciesService(speciesRepository, logService);
        Species shortName = species(1, "红树林", "被子植物门", true);
        Species longName = species(2, "红树林弹涂鱼", "脊索动物门", true);
        // 故意按 id 升序返回，模拟仓储只按 id 排序的情形
        when(speciesRepository.findMentionedIn(any(), any(), any())).thenReturn(List.of(shortName, longName));

        assertThat(svc.findMentionedIn("红树林弹涂鱼在哪里出现过", Role.ADMIN, 8))
                .extracting(Species::getChineseName)
                .containsExactly("红树林弹涂鱼", "红树林");

        EcosystemService ecoService = new EcosystemService(ecosystemRepository, logService);
        Ecosystem ecoShort = new Ecosystem();
        ecoShort.setId(1);
        ecoShort.setName("红树林");
        Ecosystem ecoLong = new Ecosystem();
        ecoLong.setId(2);
        ecoLong.setName("红树林湿地");
        // 排序已下沉到查询（order by length(name) desc）：仓储按契约返回「最长优先」的结果，
        // 服务只取第一条。这里模拟查询返回顺序，断言服务信任查询排序而非再在 Java 里排一次。
        when(ecosystemRepository.findMentionedIn(any(), any())).thenReturn(List.of(ecoLong, ecoShort));

        assertThat(ecoService.findMentionedIn("请总结红树林湿地生态系统中的物种变化").getName())
                .isEqualTo("红树林湿地");
    }

    // ===== 兜底检索的诚实性 与 失败留痕 =====

    @Test
    @DisplayName("片段兜底命中时要如实告诉模型「这是模糊搜来的」，否则问 A 答 B")
    void fallbackHitIsDisclosedToTheModel() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        when(client.textModel()).thenReturn("deepseek-v4.1-flash");
        when(ecosystemService.findMentionedIn(any())).thenReturn(null);
        when(speciesService.findMentionedIn(any(), any(), anyInt())).thenReturn(List.of());
        // 真实跑出来过的情形：库里没有鲎，片段「中国」却命中了「中国对虾」
        when(speciesService.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(species(3, "中国对虾", "节肢动物门", true)), 1, 0, 8));
        when(observationService.search(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 0, 8));
        when(client.chatText(any(), any())).thenReturn("本库未收录该物种");

        Map<String, Object> result =
                ai.ask(new AuthForms.Ask("中国鲎的分布情况如何"), user(Role.STUDENT));

        String context = String.valueOf(result.get("context"));
        assertThat(context).contains("中国对虾");
        assertThat(context)
                .as("上下文必须标明这条记录来自模糊片段，而不是用户点名的物种")
                .contains("模糊检索")
                .contains("不代表用户点名的物种在本库中存在");
    }

    @Test
    @DisplayName("大模型调用失败也要落一条 success=false 的记录：否则审计表里永远只有成功")
    void failedModelCallIsStillAudited() {
        AiService ai = new AiService(client, aiRecordRepository, chatMessageRepository,
                speciesService, observationService, ecosystemService);

        when(client.textModel()).thenReturn("deepseek-v4.1-flash");
        when(client.chatText(any(), any()))
                .thenThrow(new BizException("大模型接口返回 401：invalid_api_key"));

        assertThatThrownBy(() -> ai.complete(new AuthForms.Complete("中国鲎"), user(Role.RESEARCHER)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("401");

        ArgumentCaptor<AiRecord> saved = ArgumentCaptor.forClass(AiRecord.class);
        verify(aiRecordRepository).save(saved.capture());
        assertThat(saved.getValue().getSuccess()).isFalse();
        assertThat(saved.getValue().getResult()).contains("invalid_api_key");
        assertThat(saved.getValue().getType()).isEqualTo(com.gdou.marinebio.entity.AiType.COMPLETE);
    }

    // ===== 观测记录这一侧的 is_public：同一物种的两条读取通路必须给出同样的结论 =====

    @Test
    @DisplayName("按未公开物种 id 筛观测记录要查不到：这条通路原先完全不校验 is_public，实测公众能拿到坐标与备注")
    void searchByHiddenSpeciesIsBlockedForLowRoles() {
        ObservationService svc = new ObservationService(observationRepository,
                org.mockito.Mockito.mock(ObservationSpeciesRepository.class),
                speciesRepository, ecosystemService, logService);
        when(speciesRepository.findById(16)).thenReturn(Optional.of(species(16, "白斑刺鲀", "脊索动物门", false)));
        when(observationRepository.search(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        for (Role role : List.of(Role.PUBLIC, Role.STUDENT)) {
            assertThat(svc.search(role, null, null, 16L, null, null, null, Pageable.ofSize(10)).getTotal())
                    .as("%s 按隐藏物种检索应为空页", role)
                    .isZero();
        }
        // 关键不只是「结果为空」，而是一次都不该去查观测表：
        // 原先 ObservationRepository 的 exists 子查询只按 species_id 匹配、不 join species，
        // GET /api/observations?speciesId=16 对公众直接返回 3 条，含地点名、经纬度与备注原文，
        // 而同一个物种的 GET /api/species/16 回的是「物种不存在或无权查看」。
        verify(observationRepository, org.mockito.Mockito.never())
                .search(any(), any(), any(), any(), any(), any(), any());

        // 不存在的 id 也必须给出同一个空页，否则这就是一个免口令的存在性探针
        when(speciesRepository.findById(999)).thenReturn(Optional.empty());
        assertThat(svc.search(Role.PUBLIC, null, null, 999L, null, null, null, Pageable.ofSize(10))
                .getTotal()).isZero();

        // 有权角色不受影响
        svc.search(Role.RESEARCHER, null, null, 16L, null, null, null, Pageable.ofSize(10));
        verify(observationRepository).search(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("遮蔽未公开物种时必须整行遮掉：speciesId/count/behavior/note 原先原样下发")
    void hiddenSpeciesRowIsMaskedCompletely() {
        ObservationSpeciesRepository links = org.mockito.Mockito.mock(ObservationSpeciesRepository.class);
        ObservationService svc = new ObservationService(observationRepository, links,
                speciesRepository, ecosystemService, logService);

        Species hidden = species(18, "粗纹鹅卵石斑鱼", "脊索动物门", false);
        hidden.setScientificName("Orectochilus sp.");
        ObservationSpecies link = new ObservationSpecies();
        link.setId(27);
        link.setSpecies(hidden);
        link.setCount(10);
        link.setBehavior("跳跃、滑行于岩缝");
        link.setNote("仅见于潮缝，数量极少");
        when(links.findByObservationId(7)).thenReturn(List.of(link));

        Map<String, Object> masked = svc.listLinkedSpecies(7, Role.PUBLIC).get(0);
        assertThat(masked.get("chineseName")).isEqualTo("未公开物种");
        // 这三样在上一版里是原样发出的，实测返回里能看到 "speciesId":18,"count":10,"behavior":"跳跃、滑行于岩缝"
        assertThat(masked.get("speciesId")).isEqualTo(-1);
        assertThat(masked.get("count")).isNull();
        assertThat(masked.get("behavior")).isNull();
        assertThat(masked.get("note")).isNull();
        // 行数本身保留：否则「本次观测到 N 个物种」的计数会反过来暴露隐藏条数
        assertThat(masked.get("linkId")).isEqualTo(27);

        Map<String, Object> full = svc.listLinkedSpecies(7, Role.RESEARCHER).get(0);
        assertThat(full.get("chineseName")).isEqualTo("粗纹鹅卵石斑鱼");
        assertThat(full.get("speciesId")).isEqualTo(18);
        assertThat(full.get("count")).isEqualTo(10);
    }

    private static String n(int length) {
        return "x".repeat(length);
    }
}
