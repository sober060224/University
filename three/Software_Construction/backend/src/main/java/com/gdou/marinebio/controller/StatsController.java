package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模块四：综合数据看板。一次返回概览指标 + 全部图表数据，前端无需拼多个请求。
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@AuthenticationPrincipal LoginUser loginUser) {
        // 角色传进聚合层，未公开物种的条数不进入学生/公众看到的统计
        return Result.ok(statsService.dashboard(loginUser == null ? null : loginUser.getRole()));
    }
}
