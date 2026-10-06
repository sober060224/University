package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Species;
import com.gdou.marinebio.service.SpeciesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

import java.util.List;
import java.util.Map;

/**
 * 模块二：物种信息管理。系统最核心的数据模块。
 * 列表与详情对未登录访客开放，但只能看到 is_public=1 的数据（由 Service 层过滤）。
 */
@RestController
@RequestMapping("/api/species")
@RequiredArgsConstructor
public class SpeciesController {

    private final SpeciesService speciesService;

    @GetMapping
    public Result<PageResult<Species>> list(@AuthenticationPrincipal LoginUser loginUser,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String phylum,
                                            @RequestParam(required = false) String protectionLevel,
                                            @RequestParam(required = false) String endangerStatus,
                                            @RequestParam(required = false) String distribution,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageResult.of(page, size, 100, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(speciesService.search(LoginUser.roleOf(loginUser), keyword, phylum,
                protectionLevel, endangerStatus, distribution, pageable));
    }

    @GetMapping("/phylums")
    public Result<List<String>> phylums() {
        return Result.ok(speciesService.listPhylums());
    }

    /** 模块四：物种分布地图数据点 */
    @GetMapping("/map")
    public Result<List<Map<String, Object>>> map(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(speciesService.mapPoints(LoginUser.roleOf(loginUser)));
    }

    @GetMapping("/{id}")
    public Result<Species> detail(@PathVariable Integer id, @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(speciesService.get(id, LoginUser.roleOf(loginUser)));
    }

    @PostMapping
    public Result<Species> create(@Valid @RequestBody SpeciesForms.Create form,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("物种信息已新增", speciesService.create(form, loginUser));
    }

    @PutMapping("/{id}")
    public Result<Species> update(@PathVariable Integer id,
                                  @Valid @RequestBody SpeciesForms.Create form,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("物种信息已更新", speciesService.update(id, form, loginUser));
    }

    /** 删除需管理员权限（SecurityConfig 拦截），同时级联删除观测关联 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal LoginUser loginUser) {
        speciesService.delete(id, loginUser);
        return Result.ok("物种信息已删除", null);
    }
}
