package com.gdou.marinebio.dto;

import jakarta.validation.Valid;
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
     * 但 Service 的 apply() 对其余字段是「传了才改」：null 表示本次不动这个字段，
     * 空串表示清空。所以 PUT 是必填项校验 + 其余字段按需局部更新，不是整体替换。
 */
public final class SpeciesForms {

    private SpeciesForms() {
    }

    /** 新增物种；chineseName 之外的字段都可以留空后续补录 */
    public record Create(
            @NotBlank(message = "中文名不能为空") @Size(max = 100) String chineseName,
            @Size(max = 150) String scientificName,
            @Size(max = 30) String phylum,
            @Size(max = 30) String className,
            @Size(max = 30) String orderName,
            @Size(max = 30) String familyName,
            @Size(max = 30) String genusName,
            @Size(max = 30) String speciesName,
            String morphology,
            String habits,
            @Size(max = 255) String distribution,
            Double longitude,
            Double latitude,
            @Size(max = 30) String protectionLevel,
            @Size(max = 30) String endangerStatus,
            @Size(max = 255) String imageUrl,
            @Size(max = 255) String videoUrl,
            String reference,
            Boolean isPublic) {
    }

    /** 生态系统的增改共用一套字段 */
    public record Ecosystem(
            @NotBlank(message = "生态系统名称不能为空") @Size(max = 100) String name,
            @Size(max = 30) String code,
            String description,
            @Size(max = 255) String imageUrl) {
    }

    /** 观测记录，species 是一次观测中看到的所有物种 */
    public record Observation(
            @NotNull(message = "请选择观测所属的生态系统") Integer ecosystemId,
            @NotNull(message = "请填写观测时间") java.time.LocalDateTime observeTime,
            Double longitude,
            Double latitude,
            @Size(max = 200) String locationName,
            Double waterTemp,
            Double salinity,
            Double depth,
            @Size(max = 30) String weather,
            String notes,
            // 嵌套记录上的约束（SpeciesLink 的 @NotNull / @PositiveOrZero）必须靠这个 @Valid 才会级联校验
            @NotEmpty(message = "请至少关联一个物种") @Valid List<SpeciesLink> species) {
    }

    /** 观测记录里的单个物种项：记录估算数量与观察到的行为 */
    public record SpeciesLink(
            @NotNull(message = "请选择物种") Integer speciesId,
            @PositiveOrZero(message = "估算数量不能为负数") Integer count,
            @Size(max = 255) String behavior,
            @Size(max = 500) String note) {
    }
}
