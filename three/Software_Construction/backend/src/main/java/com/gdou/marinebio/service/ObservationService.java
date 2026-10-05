package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Observation;
import com.gdou.marinebio.entity.ObservationSpecies;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.Species;
import com.gdou.marinebio.repository.ObservationRepository;
import com.gdou.marinebio.repository.ObservationSpeciesRepository;
import com.gdou.marinebio.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 模块三：观测记录的创建、查询与维护，以及"观测记录—物种"多对多关联。
 *
 * 一次观测往往看到不止一种物种，关联时还要记录每种的估算数量与观察到的行为，
 * 这正是愿景文档强调的核心交互点，因此关联关系单独建表而不是塞进观测记录本身。
 */
@Service
@RequiredArgsConstructor
public class ObservationService {

    private final ObservationRepository observationRepository;
    private final ObservationSpeciesRepository observationSpeciesRepository;
    private final SpeciesRepository speciesRepository;
    private final EcosystemService ecosystemService;
    private final LogService logService;

    public PageResult<Observation> search(Long ecosystemId, Long observerId, Long speciesId,
                                          String keyword, LocalDateTime from, LocalDateTime to,
                                          Pageable pageable) {
        return PageResult.of(observationRepository.search(ecosystemId, observerId, speciesId,
                blankToNull(keyword), from, to, pageable));
    }

    /** 详情：带上所属生态系统与本次观测到的全部物种 */
    public Map<String, Object> detail(Integer id, Role role) {
        Observation observation = observationRepository.findWithEcosystemById(id)
                .orElseThrow(() -> new BizException("观测记录不存在"));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("observation", observation);
        data.put("species", listLinkedSpecies(id, role));
        return data;
    }

    @Transactional
    public Observation create(SpeciesForms.Observation form, LoginUser operator) {
        Observation observation = new Observation();
        apply(observation, form, true);
        observation.setObserverId(operator.getId());
        Observation saved = observationRepository.save(observation);
        saveLinks(saved, form.species(), operator.getRole());
        logService.record(operator, "模块三", "新增观测记录", "观测记录", saved.getId(),
                "在「" + saved.getEcosystem().getName() + "」新增观测记录，关联 " + form.species().size() + " 个物种");
        return saved;
    }

    @Transactional
    public Observation update(Integer id, SpeciesForms.Observation form, LoginUser operator) {
        Observation observation = require(id);
        checkOwnership(observation, operator);
        apply(observation, form, false);
        Observation saved = observationRepository.save(observation);
        // form.species() 带 @NotEmpty，到这里必然非空，直接整组替换关联
        observationSpeciesRepository.deleteByObservationId(id);
        saveLinks(observation, form.species(), operator.getRole());
        logService.record(operator, "模块三", "编辑观测记录", "观测记录", id, "编辑观测记录 #" + id);
        return saved;
    }

    @Transactional
    public void delete(Integer id, LoginUser operator) {
        Observation observation = require(id);
        checkOwnership(observation, operator);
        // observation_species.observation_id 是 ON DELETE CASCADE，关联项会一并删除
        observationRepository.delete(observation);
        logService.record(operator, "模块三", "删除观测记录", "观测记录", id, "删除观测记录 #" + id);
    }

    /**
     * 关联物种清单。未公开物种对非特权角色只保留 id，
     * 名称、图片这些能认出"这是什么"的字段一律不给。
     */
    public List<Map<String, Object>> listLinkedSpecies(Integer observationId, Role role) {
        boolean seeAll = role == Role.ADMIN || role == Role.RESEARCHER;
        return observationSpeciesRepository.findByObservationId(observationId).stream().map(link -> {
            Species species = link.getSpecies();
            boolean visible = seeAll || Boolean.TRUE.equals(species.getIsPublic());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("linkId", link.getId());
            item.put("speciesId", species.getId());
            item.put("chineseName", visible ? species.getChineseName() : "未公开物种");
            item.put("scientificName", visible ? species.getScientificName() : null);
            item.put("imageUrl", visible ? species.getImageUrl() : null);
            item.put("protectionLevel", visible ? species.getProtectionLevel() : null);
            item.put("count", link.getCount());
            item.put("behavior", link.getBehavior());
            item.put("note", link.getNote());
            return item;
        }).toList();
    }

    // ---------------- 模块四：观测地点地图与观测活动统计 ----------------

    /** 观测地点地图，点位上带一份概要信息供点击查看 */
    public List<Map<String, Object>> mapPoints() {
        Map<Integer, Long> speciesCount = new HashMap<>();
        for (Object[] row : observationSpeciesRepository.countGroupByObservation()) {
            speciesCount.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return observationRepository.findAllWithEcosystem().stream()
                .filter(o -> o.getLongitude() != null && o.getLatitude() != null)
                .map(o -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("id", o.getId());
                    point.put("longitude", o.getLongitude());
                    point.put("latitude", o.getLatitude());
                    point.put("locationName", o.getLocationName());
                    point.put("observeTime", o.getObserveTime());
                    point.put("ecosystem", o.getEcosystem() == null ? null : o.getEcosystem().getName());
                    point.put("speciesCount", speciesCount.getOrDefault(o.getId(), 0L));
                    return point;
                })
                .toList();
    }

    /**
     * 关联物种一次性批量取回，不再在循环里逐条 findById（一次观测 N 个物种就是 N 条 SQL）。
     * 顺便在这里按调用者角色过滤：观测记录本身受保护，但不该顺带把未公开物种的
     * 名称和图片带给没有权限的角色。
     */
    private void saveLinks(Observation observation, List<SpeciesForms.SpeciesLink> links, Role role) {
        boolean seeAll = role == Role.ADMIN || role == Role.RESEARCHER;
        List<Integer> ids = links.stream().map(SpeciesForms.SpeciesLink::speciesId).distinct().toList();
        // observation_species 上 (observation_id, species_id) 是唯一键，重复关联会撞库报 500，
        // 这里提前给出可读的提示
        if (ids.size() != links.size()) {
            throw new BizException("同一个物种不能在一次观测里重复关联");
        }
        Map<Integer, Species> visible = speciesRepository.findAllById(ids)
                .stream()
                .filter(s -> seeAll || Boolean.TRUE.equals(s.getIsPublic()))
                .collect(Collectors.toMap(Species::getId, s -> s));
        List<ObservationSpecies> entities = new ArrayList<>();
        for (SpeciesForms.SpeciesLink link : links) {
            Species species = visible.get(link.speciesId());
            if (species == null) {
                throw new BizException("物种不存在或无权关联，id=" + link.speciesId());
            }
            ObservationSpecies entity = new ObservationSpecies();
            entity.setObservation(observation);
            entity.setSpecies(species);
            entity.setCount(link.count() == null ? 0 : link.count());
            entity.setBehavior(link.behavior());
            entity.setNote(link.note());
            entities.add(entity);
        }
        observationSpeciesRepository.saveAll(entities);
    }

    private void apply(Observation target, SpeciesForms.Observation form, boolean isCreate) {
        if (isCreate || form.ecosystemId() != null) {
            target.setEcosystem(ecosystemService.get(form.ecosystemId()));
        }
        if (isCreate || form.observeTime() != null) target.setObserveTime(form.observeTime());
        if (form.longitude() != null) target.setLongitude(form.longitude());
        if (form.latitude() != null) target.setLatitude(form.latitude());
        if (form.locationName() != null) target.setLocationName(form.locationName());
        if (form.waterTemp() != null) target.setWaterTemp(form.waterTemp());
        if (form.salinity() != null) target.setSalinity(form.salinity());
        if (form.depth() != null) target.setDepth(form.depth());
        if (form.weather() != null) target.setWeather(form.weather());
        if (form.notes() != null) target.setNotes(form.notes());
    }

    /** 科研人员只能改删自己的观测记录，管理员不受限制 */
    private void checkOwnership(Observation observation, LoginUser operator) {
        if (operator.getRole() == Role.ADMIN) {
            return;
        }
        if (!Objects.equals(observation.getObserverId(), operator.getId())) {
            throw new BizException("只能修改自己提交的观测记录");
        }
    }

    private Observation require(Integer id) {
        return observationRepository.findById(id).orElseThrow(() -> new BizException("观测记录不存在"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
