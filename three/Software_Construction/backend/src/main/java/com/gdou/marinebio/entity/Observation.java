package com.gdou.marinebio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.gdou.marinebio.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 观测记录，对应模块三「观测记录创建」。
 * 一次观测可关联多个物种，关联关系见 {@link ObservationSpecies}。
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "observations")
public class Observation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 所属生态系统。LAZY 避免列表查询时每行都去查一次生态系名 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecosystem_id", nullable = false)
    private Ecosystem ecosystem;

    /** 观测人员。不设外键约束，人员被删除后观测记录仍需保留 */
    @Column(name = "observer_id")
    private Integer observerId;

    @Column(name = "observe_time", nullable = false)
    private LocalDateTime observeTime;

    @Column
    private Double longitude;

    @Column
    private Double latitude;

    @Column(name = "location_name", length = 200)
    private String locationName;

    /** 水温 ℃ */
    @Column(name = "water_temp")
    private Double waterTemp;

    /** 盐度 ‰ */
    private Double salinity;

    /** 水深 m */
    private Double depth;

    @Column(length = 30)
    private String weather;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
