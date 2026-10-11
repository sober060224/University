package com.gdou.marinebio.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
     * 模块二、模块三的请求体。
     * 增改共用同一份表单记录，POST 与 PUT 都受这里的必填约束限制，因此 PUT 也必须带中文名。
     * 但 Service 的 apply() 对其余字段是「传了才改」：null 表示本次不动它，
     * 空串表示清空。所以 PUT 是必填项校验 + 其余字段按需局部更新，不是整体替换。
     *
     * <p>长度上限一律对齐 database.sql 的列宽：TEXT 列也给了明确上限，
     * 因为不写的话一个超长请求体会一路走到 MySQL 才报「Data too long」，
     * 用户看到的是「服务暂时不可用」，比在这里直接拒掉难懂得多。
     */
public final class SpeciesForms {

    private SpeciesForms() {
    }

    /**
     * 新增物种；chineseName 之外的字段都可以留空后续补录。
     *
     * <p>每个 {@code @Size} 都写明了 message：不写的话走的是校验框架的中文默认值
     * 「个数必须在0和30之间」——「个数」说的不是字符串长度，也没说是哪个字段。
     * 而 GlobalExceptionHandler 会把同一张表单的所有失败用「；」拼成一句返回，
     * 三个字段超限时用户看到的就是「个数必须在0和30之间；个数必须在0和255之间；
     * 个数必须在0和100之间」，根本无从下手改。
     */
    public record Create(
            @NotBlank(message = "中文名不能为空")
            @Size(max = 100, message = "中文名长度不能超过 100 个字符") String chineseName,
            @Size(max = 150, message = "学名长度不能超过 150 个字符") String scientificName,
            @Size(max = 30, message = "门长度不能超过 30 个字符") String phylum,
            @Size(max = 30, message = "纲长度不能超过 30 个字符") String className,
            @Size(max = 30, message = "目长度不能超过 30 个字符") String orderName,
            @Size(max = 30, message = "科长度不能超过 30 个字符") String familyName,
            @Size(max = 30, message = "属长度不能超过 30 个字符") String genusName,
            @Size(max = 30, message = "种长度不能超过 30 个字符") String speciesName,
            @Size(max = 2000, message = "形态特征不能超过 2000 个字符") String morphology,
            @Size(max = 2000, message = "生活习性不能超过 2000 个字符") String habits,
            @Size(max = 255, message = "分布区域长度不能超过 255 个字符") String distribution,
            // 经纬度不限制范围的话，1e300 这样的值能顺利写进库，
            // 但 Leaflet 拿到之后整张地图的缩放范围会被带飞，其它点位全都看不见。
            @DecimalMin(value = "-180.0", message = "经度需在 -180 到 180 之间")
            @DecimalMax(value = "180.0", message = "经度需在 -180 到 180 之间") Double longitude,
            @DecimalMin(value = "-90.0", message = "纬度需在 -90 到 90 之间")
            @DecimalMax(value = "90.0", message = "纬度需在 -90 到 90 之间") Double latitude,
            @Size(max = 30, message = "保护等级长度不能超过 30 个字符") String protectionLevel,
            @Size(max = 30, message = "濒危度长度不能超过 30 个字符") String endangerStatus,
            @Size(max = 255, message = "图片地址长度不能超过 255 个字符") String imageUrl,
            @Size(max = 255, message = "视频链接长度不能超过 255 个字符") String videoUrl,
            @Size(max = 2000, message = "参考文献不能超过 2000 个字符") String reference,
            Boolean isPublic) {
    }

    /** 生态系统的增改共用一套字段 */
    public record Ecosystem(
            @NotBlank(message = "生态系统名称不能为空")
            @Size(max = 100, message = "生态系统名称长度不能超过 100 个字符") String name,
            @Size(max = 30, message = "编码长度不能超过 30 个字符") String code,
            @Size(max = 2000, message = "描述不能超过 2000 个字符") String description,
            @Size(max = 255, message = "图片地址长度不能超过 255 个字符") String imageUrl) {
    }

    /** 观测记录，species 是一次观测中看到的所有物种 */
    public record Observation(
            @NotNull(message = "请选择观测所属的生态系统") Integer ecosystemId,
            @NotNull(message = "请填写观测时间") java.time.LocalDateTime observeTime,
            @DecimalMin(value = "-180.0", message = "经度需在 -180 到 180 之间")
            @DecimalMax(value = "180.0", message = "经度需在 -180 到 180 之间") Double longitude,
            @DecimalMin(value = "-90.0", message = "纬度需在 -90 到 90 之间")
            @DecimalMax(value = "90.0", message = "纬度需在 -90 到 90 之间") Double latitude,
            @Size(max = 200, message = "地点名称长度不能超过 200 个字符") String locationName,
            @DecimalMin(value = "-10.0", message = "水温需在 -10 到 60 ℃ 之间")
            @DecimalMax(value = "60.0", message = "水温需在 -10 到 60 ℃ 之间") Double waterTemp,
            @DecimalMin(value = "0.0", message = "盐度需在 0 到 100 ‰ 之间")
            @DecimalMax(value = "100.0", message = "盐度需在 0 到 100 ‰ 之间") Double salinity,
            @DecimalMin(value = "-1000.0", message = "水深需在 -1000 到 11000 米之间")
            @DecimalMax(value = "11000.0", message = "水深需在 -1000 到 11000 米之间") Double depth,
            @Size(max = 30, message = "天气长度不能超过 30 个字符") String weather,
            @Size(max = 2000, message = "备注不能超过 2000 个字符") String notes,
            // 嵌套记录上的约束（SpeciesLink 的 @NotNull / @PositiveOrZero）必须靠这个 @Valid 才会级联校验。
            // 集合本身也要限条数：一个请求塞 2 万条关联，就是在一个事务里
            // deleteByObservationId + flush + saveAll(2 万)，外加 findAllById 传 2 万个 id 的巨型 IN。
            // 这与 AuthForms.Analyze.speciesNames 上的 @Size(max = 50) 治的是同一个病，此前只补了 AI 那一侧。
            @NotEmpty(message = "请至少关联一个物种")
            @Size(max = 50, message = "一次观测最多关联 50 个物种")
            @Valid List<SpeciesLink> species) {
    }

    /** 观测记录里的单个物种项：记录估算数量与观察到的行为 */
    public record SpeciesLink(
            @NotNull(message = "请选择物种") Integer speciesId,
            @PositiveOrZero(message = "估算数量不能为负数") Integer count,
            @Size(max = 255, message = "观察行为长度不能超过 255 个字符") String behavior,
            @Size(max = 500, message = "物种备注长度不能超过 500 个字符") String note) {
    }
}
