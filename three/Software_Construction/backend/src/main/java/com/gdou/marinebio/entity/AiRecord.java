package com.gdou.marinebio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.gdou.marinebio.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/**
 * 大模型调用记录。
 * 愿景文档 3.2 可靠性要求「关键识别结果需记录日志，便于追溯与质量分析」，
 * 因此每次成功/失败的调用都落库，含耗时与置信度。
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "ai_records")
public class AiRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private AiType type;

    @Column(name = "input_text", length = 1000)
    private String inputText;

    @Column(name = "input_image", length = 255)
    private String inputImage;

    @Column(columnDefinition = "TEXT")
    private String result;

    /** 0~1，图像识别的置信度 */
    private Double confidence;

    @Column(name = "target_species_id")
    private Integer targetSpeciesId;

    @Column(length = 50)
    private String model;

    /** 调用耗时（毫秒），用于核对「图像识别 5 秒内、智能问答首字 3 秒内」的性能指标 */
    @Column(name = "cost_ms")
    private Integer costMs;

    @Column
    private Boolean success = Boolean.TRUE;

    
}
