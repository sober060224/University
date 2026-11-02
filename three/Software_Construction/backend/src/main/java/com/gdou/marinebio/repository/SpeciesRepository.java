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
     *
     * <p>keyword 与 distribution 会被拼进 LIKE 的模式里，所以先把用户输入里的
     * {@code %} 与 {@code _} 转义掉（转义符用 {@code !}，同时把 {@code !} 自己也转义，
     * 不依赖各方言默认的反斜杠）：不转义的话，输入一个 {@code %} 就是「匹配全部」，
     * 输入一个 {@code _} 就是「匹配任意单字」，搜索条件形同虚设。
     */
    @Query("""
            select s from Species s where
              (:isPublic is null or s.isPublic = :isPublic)
              and (:keyword is null
                   or lower(s.chineseName) like lower(concat('%', replace(replace(replace(:keyword, '!', '!!'), '%', '!%'), '_', '!_'), '%')) escape '!'
                   or lower(s.scientificName) like lower(concat('%', replace(replace(replace(:keyword, '!', '!!'), '%', '!%'), '_', '!_'), '%')) escape '!')
              and (:phylum is null or s.phylum = :phylum)
              and (:protectionLevel is null or s.protectionLevel = :protectionLevel)
              and (:endangerStatus is null or s.endangerStatus = :endangerStatus)
              and (:distribution is null or s.distribution like concat('%', replace(replace(replace(:distribution, '!', '!!'), '%', '!%'), '_', '!_'), '%') escape '!')
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

    /**
     * 反向实体链接：找出「名字出现在了这句话里」的物种。
     *
     * <p>方向与 {@link #search} 相反。search 是「字段 like %关键词%」，适合用户只写了
     * 部分名字（「牡蛎」要能匹到「珍珠牡蛎」）；但把一整句自然语言当关键词去 like 字段
     * 永远匹配不上——字段只有 5 个字，问题有 16 个字。智能问答原来只有后一条路，
     * 于是界面里三个示例问题一个都检索不到数据，模型只能对着空上下文作答。
     *
     * <p>这里改成「问题 like %物种名%」，正是自然语言点名某个已登记实体时该有的匹配方式。
     * <p>排序按名字长度倒序而不是 id 升序：调用方用 Pageable 取前几行当候选池，
     * 池子的「取哪几条」是 SQL 决定的。原来按 id 排，一旦命中的行数超过窗口，
     * 留在池外的恰好是 id 大的那批——而更具体（名字更长）的那条往往就在里面，
     * Java 侧再怎么挑也挑不出没取回的行。
     * 候选集同样受 is_public 约束，与 matchByName 一致。
     */
    @Query("""
            select s from Species s where
              (:isPublic is null or s.isPublic = :isPublic)
              and ((s.chineseName is not null and s.chineseName <> ''
                      and :question like concat('%', s.chineseName, '%'))
                or (s.scientificName is not null and s.scientificName <> ''
                      and :question like concat('%', s.scientificName, '%')))
            order by length(coalesce(s.chineseName, s.scientificName)) desc, s.id
            """)
    List<Species> findMentionedIn(String question, Boolean isPublic, Pageable pageable);

    /**
     * 下拉框用：已登记的分类单元（门），供检索条件做二级联动。
     *
     * <p>这个接口挂在 permitAll 的 GET /api/species/phylums 上，所以同样必须带 isPublic：
     * 列表、详情、聚合统计三条路径都按可见性改写过查询条件，唯独这里漏了。
     * 只要有一个未公开物种登记了某个门类，这个门类名就会从下拉选项里漏给公众，
     * 公众由此能反推出「这个门里藏着若干条看不到的记录」。
     */
    @Query("""
            select distinct s.phylum from Species s
            where s.phylum is not null and s.phylum <> ''
              and (:isPublic is null or s.isPublic = :isPublic)
            order by s.phylum
            """)
    List<String> findDistinctPhylum(Boolean isPublic);

    /** 看板概览的物种总数，同样按可见性过滤 */
    @Query("select count(s) from Species s where (:isPublic is null or s.isPublic = :isPublic)")
    long countVisible(Boolean isPublic);

    /**
     * 物种分布地图点位：只取地图用得上的 7 个短字段，并在 SQL 里直接滤掉无坐标行。
     *
     * <p>原先走 search(..., Pageable.unpaged()) 把全表连 description/distribution 两个
     * TEXT 列整实体加载，只为拼几个字段；permitAll 接口上等于用一次全表扫描换 7 列数据。
     * shortcut: 物种量到十万级且需要按图层/密度聚合时，应改成后端聚类或矢量切片。
     */
    @Query("""
            select s.id, s.chineseName, s.scientificName, s.longitude, s.latitude,
                   s.protectionLevel, s.endangerStatus
            from Species s
            where (:isPublic is null or s.isPublic = :isPublic)
              and s.longitude is not null and s.latitude is not null
            """)
    List<Object[]> findMapPoints(Boolean isPublic);

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
