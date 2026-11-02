package com.gdou.marinebio.service;

import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.repository.OperationLogRepository;
import com.gdou.marinebio.repository.SpeciesRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 检索入参的回归测试：LIKE 模式里的通配符必须被转义。
 *
 * <p>为什么值得单独测：keyword、distribution、username 都是直接拼进
 * {@code like concat('%', :keyword, '%')} 的。用户输入一个 {@code %} 就等于「匹配全部」，
 * 输入一个 {@code _} 就等于「匹配任意单字」——搜索框还在，结果却是全表，
 * 看起来像「筛选没生效」，实际是筛选条件被吃掉了。
 *
 * <p>转义用 {@code !} 做转义符并配 {@code escape '!'}，不依赖各方言默认的反斜杠。
 *
 * <p>用真实数据库跑：{@code escape} 子句能不能被各方言正确执行、字符串函数在
 * 汉字上会不会算错，只有真跑才知道。
 */
@SpringBootTest
class SearchInputRegressionTest {

    @Autowired
    private SpeciesService speciesService;

    @Autowired
    private ObservationService observationService;

    @Autowired
    private SpeciesRepository speciesRepository;

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Test
    @DisplayName("物种检索：输入 % 不再匹配全部，输入 _ 不再匹配任意单字")
    void percentAndUnderscoreAreNotWildcards() {
        // 前置：库里确实有数据，否则「匹配不到」证明不了什么
        assertThat(speciesRepository.count()).isGreaterThan(0);

        long total = speciesService.search(Role.ADMIN, null, null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();
        assertThat(total).isGreaterThan(0);

        long withPercent = speciesService.search(Role.ADMIN, "%", null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();
        long withUnderscore = speciesService.search(Role.ADMIN, "_", null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();

        assertThat(withPercent)
                .as("百分号应被当普通字符处理，不能匹配到全部物种")
                .isZero();
        assertThat(withUnderscore)
                .as("下划线应被当普通字符处理，不能匹配到任意单字")
                .isZero();
    }

    @Test
    @DisplayName("物种检索：关键词里混着通配符时按字面匹配")
    void keywordWithWildcardIsMatchedLiterally() {
        // 含 % 的关键词只应匹配真的含「%石斑」的名字，而不是「任何含石斑的名字」
        long literal = speciesService.search(Role.ADMIN, "%石斑", null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();
        long plain = speciesService.search(Role.ADMIN, "石斑", null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();

        assertThat(literal)
                .as("含 % 的关键词只应匹配真的含「%石斑」的名字")
                .isZero();
        assertThat(plain)
                .as("不带通配符的「石斑」应当能正常命中（对照：转义没有把正常检索也打死）")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("观测记录检索：地点关键词里的 % 不再匹配全部")
    void observationKeywordWildcardIsEscaped() {
        long all = observationService.search(Role.ADMIN, null, null, null, null, null, null,
                PageResult.of(0, 100, 100, null)).getTotal();
        long withPercent = observationService.search(Role.ADMIN, null, null, null, "%", null, null,
                PageResult.of(0, 100, 100, null)).getTotal();

        assertThat(all).isGreaterThan(0);
        assertThat(withPercent)
                .as("地点关键词里的百分号应被当普通字符处理")
                .isZero();
    }

    @Test
    @DisplayName("操作日志检索：用户名里的 % 不再匹配全部")
    void logUsernameWildcardIsEscaped() {
        long all = operationLogRepository.search(null, null, null,
                PageResult.of(0, 200, 200, null)).getTotalElements();
        long withPercent = operationLogRepository.search(null, "%", null,
                PageResult.of(0, 200, 200, null)).getTotalElements();

        assertThat(all).isGreaterThan(0);
        assertThat(withPercent)
                .as("用户名里的百分号应被当普通字符处理")
                .isZero();
    }
}
