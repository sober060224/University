package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.Ecosystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EcosystemRepository extends JpaRepository<Ecosystem, Integer> {

    boolean existsByName(String name);

    Optional<Ecosystem> findByName(String name);

    /**
     * 模块四「生态系统统计：各生态系统类型的观测次数、发现的物种数」。
     * 用一次左连接把两个计数一起算出来，避免在 Java 里做两次查询再拼。
     */
    @Query(value = """
            select e.id, e.name,
                   count(distinct o.id) as observationCount,
                   count(distinct os.species_id) as speciesCount
            from ecosystems e
            left join observations o on o.ecosystem_id = e.id
            left join observation_species os on os.observation_id = o.id
            group by e.id, e.name
            order by observationCount desc
            """, nativeQuery = true)
    List<Object[]> statsWithCounts();
}
