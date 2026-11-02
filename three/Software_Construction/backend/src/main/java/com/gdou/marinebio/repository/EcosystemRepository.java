package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.Ecosystem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EcosystemRepository extends JpaRepository<Ecosystem, Integer> {

    boolean existsByName(String name);

    Optional<Ecosystem> findByName(String name);

    /**
     * 反向实体链接：找出「名字出现在了这句话里」的生态系统。
     *
     * <p>智能问答要把问题落到具体的观测记录上，而「请总结红树林生态系统中物种数量的
     * 变化趋势」这类问法点的是生态系统名，不是物种名，也不是 location_name 的子串
     * （地点名比问题长，正向 like 必然落空）。按生态系统名做反向匹配再拿 id 去过滤
     * 观测记录，是这类问法唯一能落地的路径。
     *
     * <p>「多个生态系统名同时出现在一句话里该取哪个」由调用方按名字长度倒序判定，
     * 语句本身只按 id 排序——那是业务判据，不该压在 SQL 的函数表达式上。
     */
    @Query("""
            select e from Ecosystem e where
              e.name is not null and e.name <> ''
              and :question like concat('%', e.name, '%')
            order by length(e.name) desc, e.id
            """)
    List<Ecosystem> findMentionedIn(String question, Pageable pageable);

    /**
     * 模块四「生态系统统计：各生态系统类型的观测次数、发现的物种数」。
     * 用一次左连接把两个计数一起算出来，避免在 Java 里做两次查询再拼。
     *
     * <p>publicFilter = 1 表示调用者可以看到未公开物种（管理员与科研人员），
     * 此时 speciesCount 统计全部关联物种；传 0 时只统计已公开物种。
     * 不加这个过滤的话，学生与公众角色能从「发现物种数」反推出未公开物种有多少条，
     * 与 ObservationService.listLinkedSpecies、SpeciesService.get 对未公开物种的
     * 遮蔽策略相矛盾。
     */
    @Query(value = """
            select e.id, e.name,
                   count(distinct o.id) as observationCount,
                   count(distinct case when :publicFilter = 1 or s.is_public = 1
                                       then os.species_id end) as speciesCount
            from ecosystems e
            left join observations o on o.ecosystem_id = e.id
            left join observation_species os on os.observation_id = o.id
            left join species s on s.id = os.species_id
            group by e.id, e.name
            order by observationCount desc
            """, nativeQuery = true)
    List<Object[]> statsWithCounts(@Param("publicFilter") int publicFilter);
}
