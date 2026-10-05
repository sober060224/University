package com.gdou.marinebio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.gdou.marinebio.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 物种信息，对应模块二「物种信息管理」（愿景文档标注的核心模块）
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "species")
public class Species extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "chinese_name", nullable = false, length = 100)
    private String chineseName;

    @Column(name = "scientific_name", length = 150)
    private String scientificName;

    @Column(length = 50)
    private String phylum;

    /** 纲。class 是 Java 关键字，列名沿用 database.sql 的 class_name */
    @Column(name = "class_name", length = 50)
    private String className;

    @Column(name = "order_name", length = 50)
    private String orderName;

    @Column(name = "family_name", length = 50)
    private String familyName;

    @Column(name = "genus_name", length = 50)
    private String genusName;

    @Column(name = "species_name", length = 50)
    private String speciesName;

    @Column(columnDefinition = "TEXT")
    private String morphology;

    @Column(columnDefinition = "TEXT")
    private String habits;

    @Column(length = 255)
    private String distribution;

    @Column
    private Double longitude;

    @Column
    private Double latitude;

    /** 保护等级：国家一级/国家二级/自治区重点/无 */
    @Column(name = "protection_level", length = 30)
    private String protectionLevel;

    /** 濒危度：未评估/无危/易危/濒危/极危 */
    @Column(name = "endanger_status", length = 30)
    private String endangerStatus;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "video_url", length = 255)
    private String videoUrl;

    @Column(columnDefinition = "TEXT")
    private String reference;

    /** 是否对公众开放。公众角色只能看到 isPublic=true 的物种，属于数据级权限 */
    @Column(name = "is_public")
    private Boolean isPublic = Boolean.TRUE;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 建档时也填上，跟建表语句里 update_time DEFAULT CURRENT_TIMESTAMP 的行为保持一致
    @PrePersist
    void onCreateSpecies() {
        if (updateTime == null) {
            updateTime = LocalDateTime.now().withNano(0);
        }
    }

    @PreUpdate
    void onUpdate() {
        updateTime = LocalDateTime.now().withNano(0);
    }
}
