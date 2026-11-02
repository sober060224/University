package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.Role;
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
                                                @RequestParam(defaultValue = "10") int size,
                                                @AuthenticationPrincipal LoginUser loginUser) {
        // 次级排序键补上唯一的 id：observe_time 只到分钟且不唯一，MySQL 对
        // 「ORDER BY 非唯一列 + OFFSET」不保证跨请求的稳定次序，同一行可能出现在两页、
        // 另一行永远不出现。界面翻页看不出来，但前端 fetchAllPages 靠逐页拉全量做
        // Excel 导出，顺序不稳等于静默丢行——正是上一轮修「只导第一页」时没挖到的根因。
        Pageable pageable = PageResult.of(page, size, 100,
                Sort.by(Sort.Direction.DESC, "observeTime").and(Sort.by(Sort.Direction.DESC, "id")));
        Role role = loginUser == null ? null : loginUser.getRole();
        return Result.ok(observationService.search(role, ecosystemId, observerId, speciesId,
                keyword, from, to, pageable));
    }

    /** 模块四：观测地点地图数据点 */
    @GetMapping("/map")
    public Result<List<Map<String, Object>>> map(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(observationService.mapPoints(loginUser == null ? null : loginUser.getRole()));
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
        return Result.ok("观测记录已创建", observationService.create(form, requireLogin(loginUser)));
    }

    // 更新与新增共用同一份表单（species 带 @NotEmpty），所以这里同样要 @Valid
    @PutMapping("/{id}")
    public Result<Observation> update(@PathVariable Integer id,
                                      @Valid @RequestBody SpeciesForms.Observation form,
                                      @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("观测记录已更新", observationService.update(id, form, requireLogin(loginUser)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal LoginUser loginUser) {
        observationService.delete(id, requireLogin(loginUser));
        return Result.ok("观测记录已删除", null);
    }

    /**
     * 写操作的 operator 必须非空：Service 里会直接调 operator.getId()/getRole()，
     * 传入 null 只会在很深的地方变成 NPE（而且已经先调过审计日志）。
     * 这三个路径当前由 SecurityConfig 限定为 ADMIN/RESEARCHER，理论上不会为 null，
     * 但一个 null 检查比「靠两个文件相距 150 行的规则保持一致」可靠。
     */
    private static LoginUser requireLogin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new BizException("请先登录后再操作");
        }
        return loginUser;
    }
}
