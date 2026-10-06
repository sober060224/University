package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.SpeciesForms;
import com.gdou.marinebio.entity.Ecosystem;
import com.gdou.marinebio.service.EcosystemService;
import com.gdou.marinebio.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 模块三：生态系统管理。查询对访客开放，增删改限科研人员与管理员。
 */
@RestController
@RequestMapping("/api/ecosystems")
@RequiredArgsConstructor
public class EcosystemController {

    private final EcosystemService ecosystemService;
    private final StatsService statsService;

    @GetMapping
    public Result<List<Ecosystem>> list() {
        return Result.ok(ecosystemService.list());
    }

    /** 模块四：各生态系统类型的观测次数与发现的物种数 */
    @GetMapping("/stats")
    public Result<List<Map<String, Object>>> stats(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(statsService.ecosystemStats(loginUser.getRole()));
    }

    @GetMapping("/{id}")
    public Result<Ecosystem> get(@PathVariable Integer id) {
        return Result.ok(ecosystemService.get(id));
    }

    @PostMapping
    public Result<Ecosystem> create(@Valid @RequestBody SpeciesForms.Ecosystem form,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("生态系统已新增", ecosystemService.create(form, loginUser));
    }

    @PutMapping("/{id}")
    public Result<Ecosystem> update(@PathVariable Integer id,
                                    @Valid @RequestBody SpeciesForms.Ecosystem form,
                                    @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok("生态系统已更新", ecosystemService.update(id, form, loginUser));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id, @AuthenticationPrincipal LoginUser loginUser) {
        ecosystemService.delete(id, loginUser);
        return Result.ok("生态系统已删除", null);
    }
}
