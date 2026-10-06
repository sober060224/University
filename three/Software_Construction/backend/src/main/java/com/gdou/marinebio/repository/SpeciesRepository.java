package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.Species;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpeciesRepository extends JpaRepository<Species, Integer> {

    boolean existsByChineseName(String chineseName);

    /**
     * 物种多条件组合检索（模块二第 4 项）。
     * isPublic 为数据级权限参数：公众角色传 true，只能看到对外开放的数据。
     */
    @Query("""
            select s from Species s where
              (:isPublic is null or s.isPublic = :isPublic)
              and (:keyword is null or lower(s.chineseName) like lower(concat('%', :keyword, '%'))
                   or lower(s.scientificName) like lower(concat('%', :keyword, '%')))
              and (:phylum is null or s.phylum = :phylum)
              and (:protectionLevel is null or s.protectionLevel = :protectionLevel)
              and (:endangerStatus is null or s.endangerStatus = :endangerStatus)
              and (:distribution is null or s.distribution like concat('%', :distribution, '%'))
            """)
    Page<Species> search(Boolean isPublic, String keyword, String phylum, String protectionLevel,
                         String endangerStatus, String distribution, Pageable pageable);

    /**
     * 按名称精确匹配（中文名或学名，忽略大小写）。
     *
     * <p>模块五把大模型返回的物种名回连到库里时需要这个：search 是包含匹配且没有排序，
     * 直接取前 5 条的话「最可能的那一条」只是 id 最小的任意一行，
     * 会把错的分布信息喂给异常检测。
     */
    @Query("""
            select s from Species s where
              (:isPublic is null or s.isPublic = :isPublic)
              and (lower(s.chineseName) = lower(:name) or lower(s.scientificName) = lower(:name))
            order by s.id
            """)
    List<Species> findExactByName(Boolean isPublic, String name);

    /** 下拉框用：已登记的分类单元（门），供检索条件做二级联动 */
    @Query("select distinct s.phylum from Species s where s.phylum is not null and s.phylum <> '' order by s.phylum")
    List<String> findDistinctPhylum();

    /** 看板概览的物种总数，同样按可见性过滤 */
    @Query("select count(s) from Species s where (:isPublic is null or s.isPublic = :isPublic)")
    long countVisible(Boolean isPublic);

    // ---- 模块四统计分析用的聚合查询 ----
    // 三条聚合同样带 isPublic 参数：看板对任何登录角色开放，
    // 不过滤的话未公开物种的条数会从统计图里漏出去。

    @Query(value = """
            select phylum, count(*) from species
            where phylum is not null and phylum <> ''
              and (:isPublic is null or is_public = :isPublic)
            group by phylum
            """, nativeQuery = true)
    List<Object[]> countGroupByPhylum(Boolean isPublic);

    @Query(value = """
            select protection_level, count(*) from species
            where (:isPublic is null or is_public = :isPublic)
            group by protection_level
            """, nativeQuery = true)
    List<Object[]> countGroupByProtectionLevel(Boolean isPublic);

    @Query(value = """
            select endanger_status, count(*) from species
            where (:isPublic is null or is_public = :isPublic)
            group by endanger_status
            """, nativeQuery = true)
    List<Object[]> countGroupByEndangerStatus(Boolean isPublic);
}
