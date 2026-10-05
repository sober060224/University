package com.gdou.marinebio.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * 分页结果，避免直接返回 Page 带来的冗余字段和不稳定的 JSON 结构
 */
@Getter
@Setter
@NoArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public PageResult(List<T> records, long total, int page, int size) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public static <T> PageResult<T> of(Page<T> p) {
        return new PageResult<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
    }

    public static <T, R> PageResult<R> of(Page<T> p, List<R> records) {
        return new PageResult<>(records, p.getTotalElements(), p.getNumber(), p.getSize());
    }

    /**
     * 各列表接口统一从这里取分页参数。
     * page / size 来自查询串，不能直接丢给 PageRequest.of：它对 page &lt; 0
     * 和 size &lt; 1 都会抛 IllegalArgumentException，那属于客户端错误，
     * 不该以"服务器内部错误"的形式返回。
     */
    public static Pageable of(int page, int size, int maxSize, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), maxSize);
        return sort == null ? PageRequest.of(safePage, safeSize) : PageRequest.of(safePage, safeSize, sort);
    }
}
