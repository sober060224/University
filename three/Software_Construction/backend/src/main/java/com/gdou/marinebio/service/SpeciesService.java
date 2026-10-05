package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.Species;
import com.gdou.marinebio.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块二：物种信息的增删改查、分类检索与详情展示。
 *
 * 数据层面的可见性由 is_public 字段控制：管理员与科研人员可见全部，
 * 学生、公众以及未登录访客只能看到公开的物种。
 */
@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final LogService logService;

    /** 管理员与科研人员可以看全部物种，其余角色只能看到 is_public = 1 的记录 */
    private static boolean canSeeAll(Role role) {
        return role == Role.ADMIN || role == Role.RESEARCHER;
    }

    public PageResult<Species> search(Role role, String keyword, String phylum, String protectionLevel,
                                      String endangerStatus, String distribution, Pageable pageable) {
        Boolean isPublic = canSeeAll(role) ? null : Boolean.TRUE;
        return PageResult.of(speciesRepository.search(isPublic, blankToNull(keyword), blankToNull(phylum),
                blankToNull(protectionLevel), blankToNull(endangerStatus), blankToNull(distribution), pageable));
    }

    /** 可见范围内的物种总数，供模块四看板概览使用 */
    public long countVisible(Role role) {
        return speciesRepository.countVisible(canSeeAll(role) ? null : Boolean.TRUE);
    }

    public Species get(Integer id, Role role) {
        Species species = require(id);
        if (!canSeeAll(role) && !Boolean.TRUE.equals(species.getIsPublic())) {
            // 不暴露"存在但无权限"，与未找到保持一致
            throw new BizException("物种不存在或无权查看");
        }
        return species;
    }

    @Transactional
    public Species create(SpeciesForms.Create form, LoginUser operator) {
        if (speciesRepository.existsByChineseName(form.chineseName())) {
            throw new BizException("物种「" + form.chineseName() + "」已存在");
        }
        Species species = new Species();
        apply(species, form, true);
        species.setCreatedBy(operator == null ? null : operator.getId());
        Species saved = speciesRepository.save(species);
        logService.record(operator, "模块二", "新增物种", "物种", saved.getId(), "新增物种「" + saved.getChineseName() + "」");
        return saved;
    }

    @Transactional
    public Species update(Integer id, SpeciesForms.Create form, LoginUser operator) {
        Species species = require(id);
        if (form.chineseName() != null && !form.chineseName().isBlank()
                && !form.chineseName().equals(species.getChineseName())
                && speciesRepository.existsByChineseName(form.chineseName())) {
            throw new BizException("物种「" + form.chineseName() + "」已存在");
        }
        apply(species, form, false);
        Species saved = speciesRepository.save(species);
        logService.record(operator, "模块二", "编辑物种", "物种", id, "编辑物种「" + saved.getChineseName() + "」");
        return saved;
    }

    @Transactional
    public void delete(Integer id, LoginUser operator) {
        Species species = require(id);
        // observation_species 上的外键是 ON DELETE CASCADE，关联记录会一并清理
        speciesRepository.delete(species);
        logService.record(operator, "模块二", "删除物种", "物种", id, "删除物种「" + species.getChineseName() + "」");
    }

    /** 分类单元下拉选项 */
    public List<String> listPhylums() {
        return speciesRepository.findDistinctPhylum();
    }

    // ---------------- 模块四：分布地图与统计分析的数据源 ----------------

    /** 物种分布地图的点位，只返回有坐标的物种 */
    public List<Map<String, Object>> mapPoints(Role role) {
        Boolean isPublic = canSeeAll(role) ? null : Boolean.TRUE;
        return speciesRepository.search(isPublic, null, null, null, null, null, Pageable.unpaged())
                .getContent().stream()
                .filter(s -> s.getLongitude() != null && s.getLatitude() != null)
                .map(s -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("id", s.getId());
                    point.put("name", s.getChineseName());
                    point.put("scientificName", s.getScientificName());
                    point.put("longitude", s.getLongitude());
                    point.put("latitude", s.getLatitude());
                    point.put("protectionLevel", s.getProtectionLevel());
                    point.put("endangerStatus", s.getEndangerStatus());
                    return point;
                })
                .toList();
    }

    public Map<String, List<Map<String, Object>>> distributionStats(Role role) {
        Boolean isPublic = canSeeAll(role) ? null : Boolean.TRUE;
        Map<String, List<Map<String, Object>>> stats = new LinkedHashMap<>();
        stats.put("phylum", toPairs(speciesRepository.countGroupByPhylum(isPublic)));
        stats.put("protectionLevel", toPairs(speciesRepository.countGroupByProtectionLevel(isPublic)));
        stats.put("endangerStatus", toPairs(speciesRepository.countGroupByEndangerStatus(isPublic)));
        return stats;
    }

    private List<Map<String, Object>> toPairs(List<Object[]> rows) {
        return rows.stream().map(row -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", row[0] == null ? "未填写" : row[0].toString());
            item.put("value", ((Number) row[1]).longValue());
            return item;
        }).toList();
    }

    // ---------------- 模块五：图像识别结果回连已有物种 ----------------

    /**
     * 大模型返回物种名后，用它去库里找可能对应的记录。
     * 检索语句本身就是"包含匹配"，精确名会排在最前，因此取前 5 条作为候选即可。
     *
     * <p>候选集同样受 is_public 约束：这个方法对任何登录角色开放，
     * 若不过滤，未公开的物种会从图像识别接口漏出去。
     */
    public List<Species> matchByName(String modelName, Role role) {
        if (modelName == null || modelName.isBlank()) {
            return List.of();
        }
        Boolean isPublic = canSeeAll(role) ? null : Boolean.TRUE;
        return speciesRepository.search(isPublic, modelName.trim(), null, null, null, null, Pageable.ofSize(5))
                .getContent();
    }

    private Species require(Integer id) {
        return speciesRepository.findById(id).orElseThrow(() -> new BizException("物种不存在"));
    }

    private void apply(Species target, SpeciesForms.Create form, boolean isCreate) {
        // 文本字段统一"传了才改"：空串表示清空该字段，null 表示这次不动它。
        // 以前只有 morphology/habits/reference 是这套语义，其余字段用 notBlank 判空，
        // 于是把「学名」这类字段删空再保存根本清不掉。
        // chineseName 是必填项，仍然只有非空才接受，避免把必填字段清成空串。
        if (isCreate || notBlank(form.chineseName())) target.setChineseName(form.chineseName());
        if (form.scientificName() != null) target.setScientificName(form.scientificName());
        if (form.phylum() != null) target.setPhylum(form.phylum());
        if (form.className() != null) target.setClassName(form.className());
        if (form.orderName() != null) target.setOrderName(form.orderName());
        if (form.familyName() != null) target.setFamilyName(form.familyName());
        if (form.genusName() != null) target.setGenusName(form.genusName());
        if (form.speciesName() != null) target.setSpeciesName(form.speciesName());
        if (form.morphology() != null) target.setMorphology(form.morphology());
        if (form.habits() != null) target.setHabits(form.habits());
        if (form.distribution() != null) target.setDistribution(form.distribution());
        if (form.longitude() != null) target.setLongitude(form.longitude());
        if (form.latitude() != null) target.setLatitude(form.latitude());
        if (form.protectionLevel() != null) target.setProtectionLevel(form.protectionLevel());
        if (form.endangerStatus() != null) target.setEndangerStatus(form.endangerStatus());
        if (form.imageUrl() != null) target.setImageUrl(form.imageUrl());
        if (form.videoUrl() != null) target.setVideoUrl(form.videoUrl());
        if (form.reference() != null) target.setReference(form.reference());
        if (isCreate) {
            target.setIsPublic(form.isPublic() == null || form.isPublic());
        } else if (form.isPublic() != null) {
            target.setIsPublic(form.isPublic());
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }
}
