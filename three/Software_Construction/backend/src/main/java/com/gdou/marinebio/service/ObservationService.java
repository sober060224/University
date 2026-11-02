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

    /**
     * 观测记录检索。
     *
     * <p>按物种筛观测记录时必须先问一句「这个角色看得见这个物种吗」，否则 {@code is_public}
     * 会被整条绕过：{@code ObservationRepository.search} 的 exists 子查询只按
     * {@code os.species.id = :speciesId} 匹配，从不 join species，所以公众拿
     * {@code speciesId=16}（未公开的白斑刺鲀）就能取到该物种出现过的时间、地点名、
     * 精确经纬度和科研人员备注原文——而同一个物种的详情接口回的是「物种不存在或无权查看」。
     * 同一份数据两条通路给出相反的权限结论，实测已复现。
     *
     * <p>无权或该 id 不存在一律返回空页：既堵住读取，也不留「有记录 vs total=0」
     * 这种不需要口令的存在性探针。
     */
    public PageResult<Observation> search(Role role, Long ecosystemId, Long observerId, Long speciesId,
                                          String keyword, LocalDateTime from, LocalDateTime to,
                                          Pageable pageable) {
        if (speciesId != null && !canSeeSpecies(role, speciesId)) {
            return PageResult.of(new org.springframework.data.domain.PageImpl<>(List.of(), pageable, 0));
        }
        // from > to 不是「没有数据」而是参数填反了：不拦的话查询无条件返回空页，
        // 用户看到「暂无观测记录」会去怀疑数据没了，而实际是他把两个日期框调了个个儿。
        // 等于同一天是合法的（表示当天一整天），只在严格大于时才报错。
        if (from != null && to != null && from.isAfter(to)) {
            throw new BizException("开始时间不能晚于结束时间");
        }
        return PageResult.of(observationRepository.search(ecosystemId, observerId, speciesId,
                blankToNull(keyword), from, to, pageable));
    }

    /** 该角色是否看得见这个物种：管理员与科研人员全量，其余只看 is_public = 1。 */
    private boolean canSeeSpecies(Role role, Long speciesId) {
        if (role == Role.ADMIN || role == Role.RESEARCHER) {
            return true;
        }
        // 不能写成 speciesId.intValue()：Long 转 int 是截断而不是溢出检查，
        // 4294967297 会被截成 1，于是「查有没有 4294967297 这个未公开物种」
        // 变成了「查 1 号物种是不是公开的」。这条路径目前 fail-closed（最多把
        // 结果错误地清空），但一个越界的入参能改变落到的行，本身就是隐患。
        if (speciesId < Integer.MIN_VALUE || speciesId > Integer.MAX_VALUE) {
            return false;
        }
        return speciesRepository.findById(speciesId.intValue())
                .filter(s -> Boolean.TRUE.equals(s.getIsPublic()))
                .isPresent();
    }

    /** 详情：带上所属生态系统与本次观测到的全部物种。
     *
     * <p>「notes」是科研人员的自由文本，可能写出物种名、地点、时间等足以
     * 反推「这是什么物种」的内容；未公开物种的观测必须连 notes 一并遮蔽，
     * 否则 detail 就是一条完整的可见性泄露链（listLinkedSpecies 已遮蔽物种列表，
     * 但 notes 漏了）。管理员与科研人员看全部，其余角色只看已公开物种的观测，
     * 其 notes 置 null。
     */
    public Map<String, Object> detail(Integer id, Role role) {
        Observation observation = observationRepository.findWithEcosystemById(id)
                .orElseThrow(() -> new BizException("观测记录不存在"));
        boolean seeAll = role == Role.ADMIN || role == Role.RESEARCHER;
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> obs = new LinkedHashMap<>();
        obs.put("id", observation.getId());
        obs.put("ecosystemId", observation.getEcosystem() == null ? null : observation.getEcosystem().getId());
        obs.put("observeTime", observation.getObserveTime());
        obs.put("longitude", observation.getLongitude());
        obs.put("latitude", observation.getLatitude());
        obs.put("locationName", observation.getLocationName());
        obs.put("waterTemp", observation.getWaterTemp());
        obs.put("salinity", observation.getSalinity());
        obs.put("depth", observation.getDepth());
        obs.put("weather", observation.getWeather());
        obs.put("notes", seeAll ? observation.getNotes() : null);
        data.put("observation", obs);
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
        // form.species() 带 @NotEmpty，到这里必然非空，直接整组替换关联。
        // 必须先 flush 让删除真正落库再插入新关联，否则 IDENTITY 主键会让 INSERT
        // 抢在 DELETE 前面执行，撞 unique_observation_species 唯一键。
        observationSpeciesRepository.deleteByObservationId(id);
        observationSpeciesRepository.flush();
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
            // 无权角色看到的必须是整行的遮蔽，而不只是换一个名字。
            // 原先只遮 chineseName/scientificName/imageUrl/protectionLevel，
            // 而 speciesId、count、behavior、note 原样下发，实测能拿到
            // {"speciesId":18,"chineseName":"未公开物种","count":10,"behavior":"跳跃、滑行于岩缝","note":"…"}：
            // ①科研人员写在隐藏物种上的自由文本本身就足以认出物种与地点，遮名字等于没遮；
            // ②更要命的是 speciesId 被直接交到无权角色手里，而 /api/observations?speciesId= 曾是
            //   一条不做可见性校验的通路——两个缺陷拼起来就是一条完整的泄露链。
            // 行数保留（否则「本次观测到 N 个物种」的计数会反过来泄露隐藏条数），只把内容清空。
            // speciesId 不能置 null：SpeciesLink.speciesId 带 @NotNull，编辑页把 detail 的
            // 物种清单原样回填成表单再提交，null 会被嵌套校验拦下，报的是「请选择物种」——
            // 而列表里明明每行都选好了物种，用户无从下手。置成 -1 而不是真 id：
            // 既不把隐藏物种的 id 交给无权角色，又能在 saveLinks 里被当成不可见物种拦下
            // （那里按 visible 映射查，查不到就抛业务异常），不会真被写进 observation_species。
            item.put("speciesId", visible ? species.getId() : -1);
            item.put("chineseName", visible ? species.getChineseName() : "未公开物种");
            item.put("scientificName", visible ? species.getScientificName() : null);
            item.put("imageUrl", visible ? species.getImageUrl() : null);
            item.put("protectionLevel", visible ? species.getProtectionLevel() : null);
            item.put("count", visible ? link.getCount() : null);
            item.put("behavior", visible ? link.getBehavior() : null);
            item.put("note", visible ? link.getNote() : null);
            return item;
        }).toList();
    }

    /**
     * 批量数一批观测各自的「可见关联物种数」。
     *
     * <p>智能问答要用这个数填上下文；逐条调 listLinkedSpecies 会退化成 N+1：
     * 8 条记录就是 8 条 join fetch 查询。未公开物种对非特权角色不计入，
     * 与 listLinkedSpecies 把隐藏物种遮蔽成占位条的口径一致。
     */
    public Map<Integer, Long> countLinkedSpecies(List<Integer> observationIds, Role role) {
        if (observationIds == null || observationIds.isEmpty()) {
            return Map.of();
        }
        boolean seeAll = role == Role.ADMIN || role == Role.RESEARCHER;
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : observationSpeciesRepository.countGroupByObservations(observationIds, seeAll)) {
            counts.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

    // ---------------- 模块四：观测地点地图与观测活动统计 ----------------

    /**
     * 观测地点地图，点位上带一份概要信息供点击查看。
     *
     * <p>speciesCount 必须按角色过滤。它原本来自 countGroupByObservation()，
     * 那个分组查询在 SQL 里数的是 observation_species 的全部关联行，不看 is_public；
     * 而 /api/observations/map 对已登录的任何角色开放（含 PUBLIC），于是
     * 「某点关联了 3 个物种、其中 2 个是未公开物种」会原样下发，
     * 与 listLinkedSpecies 的遮蔽、EcosystemRepository.statsWithCounts 的 publicFilter
     * 是同一条泄露链上的第三个洞：地图上点一下就能反推出藏着几个物种。
     */
    public List<Map<String, Object>> mapPoints(Role role) {
        boolean seeAll = role == Role.ADMIN || role == Role.RESEARCHER;
        Map<Integer, Long> speciesCount = new HashMap<>();
        for (Object[] row : observationSpeciesRepository.countGroupByObservation(seeAll)) {
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
        if (isCreate || form.observeTime() != null) {
            // @DecimalMin 不支持 LocalDateTime，只能在服务层卡边界。observe_time 是
            // MySQL DATETIME：只接受 1000-01-01 ~ 9999-12-31，而 LocalDateTime 能表示的
            // 范围比它大得多。不拦的话 0001-01-01 能过校验，直到底层驱动才报错，
            // 用户看到的是「服务器内部错误」而不是「时间填错了」。
            java.time.LocalDateTime time = form.observeTime();
            if (time != null && (time.getYear() < 1000 || time.getYear() > 9999)) {
                throw new BizException("观测时间需在 1000 年到 9999 年之间");
            }
            target.setObserveTime(time);
        }
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

    /**
     * 取一条观测记录用于编辑或删除。
     *
     * <p>必须走 fetch join：application.yml 里 open-in-view 是 false，
     * 而 Observation.ecosystem 是 LAZY。改用 findById 的话，返回值里的 ecosystem
     * 仍是未初始化的 Hibernate 代理，事务一提交 Jackson 就找不到它的序列化器，
     * PUT 接口整个响应 500（No serializer found for ByteBuddyInterceptor）。
     * update 还要把实体原样返回给前端，所以这里必须取完整的。
     */
    private Observation require(Integer id) {
        return observationRepository.findWithEcosystemById(id)
                .orElseThrow(() -> new BizException("观测记录不存在"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
