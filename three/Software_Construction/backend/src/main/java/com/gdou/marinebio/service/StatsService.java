package com.gdou.marinebio.service;

import com.gdou.marinebio.repository.EcosystemRepository;
import com.gdou.marinebio.repository.ObservationRepository;
import com.gdou.marinebio.repository.UserRepository;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块四：数据可视化与报表 —— 统计聚合服务。
 *
 * 看板与各类图表的数据全部由这里一次算好返回，避免前端为画一张图发七八个请求。
 * 所有聚合都下沉成 SQL 的 GROUP BY（见各 Repository 的 nativeQuery），Java 侧只做整形。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final ObservationRepository observationRepository;
    private final EcosystemRepository ecosystemRepository;
    private final UserRepository userRepository;
    private final SpeciesService speciesService;

    /**
     * 综合数据看板：系统概览 + 三张分布图 + 生态/时间/人员三组统计，前端一次拿全。
     *
     * <p>物种相关的三处统计都按调用者角色做数据级过滤：看板对所有登录角色开放，
     * 不带角色参数的话未公开物种的条数会从统计图里漏出去。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(Role role) {
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("speciesTotal", speciesService.countVisible(role));
        overview.put("observationTotal", observationRepository.count());
        overview.put("ecosystemTotal", ecosystemRepository.count());
        overview.put("userTotal", userRepository.count());
        overview.put("observationThisMonth", observationRepository.countByObserveTimeGreaterThanEqual(monthStart));
        overview.put("observationLast30Days", observationRepository.countByObserveTimeGreaterThanEqual(thirtyDaysAgo));
        overview.put("researcherTotal", userRepository.countByRole(Role.RESEARCHER));
        overview.put("pendingUserTotal", userRepository.countByStatus(UserStatus.PENDING));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        // 各分类单元 / 保护等级 / 濒危度占比，数据来自模块二
        result.putAll(speciesService.distributionStats(role));
        result.put("ecosystemStats", toEcosystemStats(role));
        result.put("monthlyStats", toPairs(observationRepository.countByMonth()));
        result.put("observerStats", toPairs(observationRepository.countByObserver()));
        return result;
    }

    /**
     * 各生态系统类型的观测次数与发现的物种数（模块三数据）。
     *
     * <p>和看板一样按角色过滤：学生与公众看到的「发现物种数」只数已公开物种，
     * 否则能从这个数字反推出该生态系统里还藏着多少未公开物种。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> ecosystemStats(Role role) {
        return toEcosystemStats(role);
    }

    /** 管理员与科研人员可见全部物种，其余角色只统计已公开的 */
    private static boolean seesAllSpecies(Role role) {
        return role == Role.ADMIN || role == Role.RESEARCHER;
    }

    private List<Map<String, Object>> toEcosystemStats(Role role) {
        int publicFilter = seesAllSpecies(role) ? 1 : 0;
        return ecosystemRepository.statsWithCounts(publicFilter).stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row[0]);
            item.put("name", row[1]);
            item.put("observationCount", row[2]);
            item.put("speciesCount", row[3]);
            return item;
        }).toList();
    }

    /** 把 nativeQuery 返回的 [label, count] 二元组转成 ECharts 直接可用的 {name, value}。 */
    private List<Map<String, Object>> toPairs(List<Object[]> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", String.valueOf(row[0]));
            item.put("value", row[1]);
            return item;
        }).toList();
    }
}
