package com.gdou.marinebio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.gdou.marinebio.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/**
 * 操作日志，对应模块一「用户活动日志记录」与模块二「物种信息删除（需权限审核或记录操作日志）」。
 * 关键识别结果也写入 ai_records，便于追溯与质量分析。
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "operation_logs")
public class OperationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    /** 冗余存一份用户名，用户被删除后日志仍可读 */
    @Column(length = 50)
    private String username;

    /** 模块一~模块五 */
    @Column(length = 30)
    private String module;

    @Column(length = 50)
    private String operation;

    @Column(name = "target_type", length = 30)
    private String targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(length = 500)
    private String detail;

    @Column(length = 50)
    private String ip;
}
