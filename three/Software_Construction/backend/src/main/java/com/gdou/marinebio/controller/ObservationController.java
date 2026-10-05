package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Observation;
import com.gdou.marinebio.service.ObservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模块三：观测记录管理，含「观测记录—物种」多对多关联（愿景文档标注的核心交互点）。
 * 记录可以增删改查，其中删除与修改还要判断是否为本人的记录（见 Service 的 checkOwnership）。
 */
@RestController
@RequestMapping("/api/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    @GetMapping
    public Result<PageResult<Observation>> list(@RequestParam(required = false) Long ecosystemId,
                                                @RequestParam(required = false) Long observerId,
                                                @RequestParam(required = false) Long speciesId,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageResult.of(page, size, 100, Sort.by(Sort.Direction.DESC, "observeTime"));
        return Result.ok(observationService.search(ecosystemId, observerId, speciesId, keyword, from, to, pageable));
    }

    /** 模块四：观测地点地图数据点 */
    @GetMapping("/map")
    public Result<List<Map<String, Object>>> map() {
        return Result.ok(observationService.mapPoints());
    }

    /** 详情：一次返回观测记录本体 + 关联的物种列表 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Integer id,
                                               @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(observationService.detail(id, loginUser == null ? null : loginUser.getRole()));
    }

    @PostMapping
    public Result<Observation> create(@Valid @RequestBody SpeciesForms.Observation form,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("观测记录已创建", observationService.create(form, loginUser));
    }

    // 更新与新增共用同一份表单（species 带 @NotEmpty），所以这里同样要 @Valid
    @PutMapping("/{id}")
    public Result<Observation> update(@PathVariable Integer id,
                                      @Valid @RequestBody SpeciesForms.Observation form,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("观测记录已更新", observationService.update(id, form, loginUser));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal LoginUser loginUser) {
        observationService.delete(id, loginUser);
        return Result.ok("观测记录已删除", null);
    }
}
