package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.entity.OperationLog;
import com.gdou.marinebio.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模块一：操作日志查询，仅管理员可见。
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogRepository operationLogRepository;

    @GetMapping
    public Result<PageResult<OperationLog>> list(@RequestParam(required = false) String module,
                                                 @RequestParam(required = false) String username,
                                                 @RequestParam(required = false) String operation,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageResult.of(page, size, 200, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(PageResult.of(operationLogRepository.search(module, username, operation, pageable)));
    }
}
