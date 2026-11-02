package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ObservationRepository extends JpaRepository<Observation, Integer> {

    /**
     * 观测记录多条件检索（模块三第 4 项）。
     * 用 left join 而不是 exists 子查询，是因为同一屏还要把关联的物种带出来给前端展示数量。
     *
     * <p>keyword 会拼进 LIKE 的模式，所以先把用户输入里的 % 与 _ 转义掉（转义符用 !）：
     * 不转义的话输入一个 % 就是「匹配全部」，地点检索形同虚设。
     */
    @Query(value = """
            select distinct o from Observation o
            left join fetch o.ecosystem e
            where (:ecosystemId is null or e.id = :ecosystemId)
              and (:observerId is null or o.observerId = :observerId)
              and (:speciesId is null or exists (
                    select 1 from ObservationSpecies os
                    where os.observation = o and os.species.id = :speciesId))
              and (:keyword is null or o.locationName like concat('%', replace(replace(replace(:keyword, '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')
              and (:from is null or o.observeTime >= :from)
              and (:to is null or o.observeTime <= :to)
            """, countQuery = """
            select count(distinct o) from Observation o
            where (:ecosystemId is null or o.ecosystem.id = :ecosystemId)
              and (:observerId is null or o.observerId = :observerId)
              and (:speciesId is null or exists (
                    select 1 from ObservationSpecies os
                    where os.observation = o and os.species.id = :speciesId))
              and (:keyword is null or o.locationName like concat('%', replace(replace(replace(:keyword, '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')
              and (:from is null or o.observeTime >= :from)
              and (:to is null or o.observeTime <= :to)
            """)
    Page<Observation> search(Long ecosystemId, Long observerId, Long speciesId, String keyword,
                             LocalDateTime from, LocalDateTime to, Pageable pageable);

    @Query("select o from Observation o left join fetch o.ecosystem where o.id = :id")
    Optional<Observation> findWithEcosystemById(Integer id);

    /** 观测地点地图需要一次性取出全部带生态系统的记录 */
    @Query("select distinct o from Observation o left join fetch o.ecosystem")
    List<Observation> findAllWithEcosystem();

    // ---- 模块四观测活动统计 ----

    /**
     * 月度观测趋势。
     *
     * <p>date_format 是 MySQL 方言，不跟移植性：本项目定位就是 MySQL（lab01/database.sql
     * 的 ENGINE=InnoDB/utf8mb4 同样 MySQL 专属），真要换库改的是整套 native 查询而不只这一句。
     */
    @Query(value = "select date_format(observe_time, '%Y-%m') as ym, count(*) as c from observations group by ym order by ym", nativeQuery = true)
    List<Object[]> countByMonth();

    /**
     * 各观测人员 Top10。
     * group by 必须带上 u.id：只按 real_name 分组会把同名的两个人合并成一根柱子，
     * 统计出的人数和条数都不是真的。order by 补 u.id 是为了让并列时结果稳定。
     *
     * <p>已删账号的观测归入「未知人员」：observations.observer_id 是 ON DELETE SET NULL，
     * 而系统里根本没有删号接口（UserController 无 DELETE），注销只走 status=DISABLED，
     * 那人还在表里、名字照带；只有直接改库删行才会出现 SET NULL。届时把历史记录归属
     * 某个具体的人反而更难解释，归「未知人员」是有意选择。
     */
    @Query(value = """
        select coalesce(u.real_name, '未知人员') as name, count(*) as c
        from observations o left join users u on u.id = o.observer_id
        group by u.id, u.real_name order by c desc, u.id limit 10
            """, nativeQuery = true)
    List<Object[]> countByObserver();

    /** 看板上的「本月观测数」「近 30 天观测数」 */
    long countByObserveTimeGreaterThanEqual(LocalDateTime time);
}
