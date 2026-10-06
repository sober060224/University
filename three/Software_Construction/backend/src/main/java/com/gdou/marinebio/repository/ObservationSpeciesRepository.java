package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.ObservationSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObservationSpeciesRepository extends JpaRepository<ObservationSpecies, Integer> {

    /**
     * 取一次观测的全部关联项。
     *
     * <p>必须 join fetch os.species：ObservationSpecies.species 是 EAGER，
     * 但 EAGER 只在 Hibernate 认为「一次能取完」时才会合并成一条语句；
     * 这里返回的是列表，Hibernate 会退化成逐行补查（1 + N 条 SQL）。
     * 实测一条关联 4 个物种的观测记录要发 5 条 SELECT，其中 4 条是逐个查 species。
     * 显式 join fetch 后固定为 2 条。
     */
    @Query("select os from ObservationSpecies os join fetch os.species where os.observation.id = :observationId")
    List<ObservationSpecies> findByObservationId(@Param("observationId") Integer observationId);

    /**
     * 删除这次观测原有的全部关联项。
     *
     * <p>注意：这个派生删除只把 em.remove() 排进动作队列、并不立刻落库，
     * 而 ObservationSpecies 用 IDENTITY 主键，saveLinks 里的 persist() 会马上执行
     * INSERT 取回自增 id，于是新插入先于旧删除写进库，撞上 database.sql 的
     * unique_observation_species，编辑一条记录必然报 Duplicate entry。
     * 所以调用方必须在插入新关联前显式 flush()。
     *
     * <p>这里不用 @Modifying bulk delete：它绕过 Hibernate 读张表，
     * 又得靠 clearAutomatically 清上下文，而一清，context 里的 Observation.ecosystem
     * 就成了游离的 Hibernate 代理，返回给前端时 Jackson 直接报
     * No serializer found for class ByteBuddyInterceptor，整个响应 500。
     */
    void deleteByObservationId(Integer observationId);

    /** 某物种被多少条观测记录关联，删除物种前判断影响面用 */
    long countBySpeciesId(Integer speciesId);

    /** 地图上要显示每条记录关联了几个物种，一次分组查询代替逐条查询 */
    @Query("select os.observation.id, count(os.id) from ObservationSpecies os group by os.observation.id")
    List<Object[]> countGroupByObservation();
}
