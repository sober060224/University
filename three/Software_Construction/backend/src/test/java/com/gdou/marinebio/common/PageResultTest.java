package com.gdou.marinebio.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 分页参数归一化的回归测试。
 *
 * <p>报告的 表 8-1 原来把接口分页写成「page 从 1 开始」，而所有控制器用的都是
 * Spring Data 的 0 基页码——照文档对接的人会整整错开一页。文档已经改对了，
 * 这里把行为本身钉住：0 基、越界归一化、以及 size 的上限夹值。
 */
class PageResultTest {

    @Test
    @DisplayName("接口页码是 0 基：page=0 就是第一页")
    void pageIsZeroBased() {
        var pageable = PageResult.of(0, 10, 100, Sort.by(Sort.Direction.DESC, "id"));

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("负数页码与非法每页条数被归一化，不会把 IllegalArgumentException 抛成 500")
    void outOfRangeParamsAreNormalized() {
        // PageRequest.of 对 page<0 和 size<1 都会抛 IllegalArgumentException，
        // 那属于客户端错误，不该以「服务器内部错误」的形式回给前端
        assertThat(PageResult.of(-5, 10, 100, null).getPageNumber()).isZero();
        assertThat(PageResult.of(3, 0, 100, null).getPageSize()).isEqualTo(1);
        assertThat(PageResult.of(3, -1, 100, null).getPageSize()).isEqualTo(1);

        assertThatCode(() -> PageResult.of(Integer.MIN_VALUE, Integer.MIN_VALUE, 100, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("每页条数按上限夹值：物种列表 100，日志 200")
    void sizeIsClampedToMax() {
        assertThat(PageResult.of(0, 1000, 100, null).getPageSize()).isEqualTo(100);
        assertThat(PageResult.of(0, 9999, 200, null).getPageSize()).isEqualTo(200);
        // 夹值而不是拒绝：传大了只会少拿一点，不会报错
        assertThat(PageResult.of(0, 50, 100, null).getPageSize()).isEqualTo(50);
    }

    @Test
    @DisplayName("响应体里回显的 page 与请求一致，前端才能对上页码")
    void echoesRequestedPage() {
        var pageable = PageResult.of(1, 10, 100, null);
        // 夹具必须自洽：offset 20 的那一页只放 1 条数据时，PageImpl 会认为这个组合不成立
        // 并自己改写 totalElements（25 变成 21），测出来的就不是 PageResult 的行为而是 PageImpl 的兜底。
        var page = new PageImpl<>(List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"), pageable, 25);

        PageResult<String> result = PageResult.of(page);

        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotal()).isEqualTo(25);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getRecords()).hasSize(10);
    }

    @Test
    @DisplayName("总页数按 size 向上取整，size 非法时回 0 而不是除零")
    void totalPagesRoundsUp() {
        assertThat(new PageResult<>(List.of(), 21, 0, 10).getTotalPages()).isEqualTo(3);
        assertThat(new PageResult<>(List.of(), 20, 0, 10).getTotalPages()).isEqualTo(2);
        assertThat(new PageResult<>(List.of(), 0, 0, 0).getTotalPages()).isZero();
    }

    @Test
    @DisplayName("统一响应体的三种构造：成功、带消息的成功、失败")
    void resultFactoriesShapeTheEnvelope() {
        assertThat(Result.ok("data").isSuccess()).isTrue();
        assertThat(Result.ok("data").getMessage()).isEqualTo("操作成功");
        assertThat(Result.ok("已保存", "data").getMessage()).isEqualTo("已保存");

        assertThat(Result.fail("物种不存在").isSuccess()).isFalse();
        assertThat(Result.fail("物种不存在").getData()).isNull();
        assertThat(Result.<String>fail("物种不存在").getMessage()).isEqualTo("物种不存在");
    }
}
