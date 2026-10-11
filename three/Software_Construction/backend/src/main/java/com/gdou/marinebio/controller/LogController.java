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
        // module / operation 在 JPQL 里是等值比较，空串不是「不筛选」而是「筛选等于空串」，
        // 于是前端把未选中的下拉传成 ?module= 时，列表会一片空白，看不出是权限问题还是没数据。
        // 关键词那一路走的是 like，空串恰好等价于全选，所以只有这两个等值条件需要归一化。
        return Result.ok(PageResult.of(operationLogRepository.search(
                blankToNull(module), blankToNull(username), blankToNull(operation), pageable)));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
