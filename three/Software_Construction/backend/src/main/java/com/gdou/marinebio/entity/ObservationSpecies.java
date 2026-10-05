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
import jakarta.persistence.UniqueConstraint;
import com.gdou.marinebio.common.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/**
 * 观测记录与物种的关联关系。
 *
 * <p>愿景文档 3.1 模块三明确标注「观测-物种关联……并记录每个物种的估算数量、行为等详细信息。
 * （这是核心交互点）」，因此这里不是纯连接表，而是带业务属性的关联实体。
 * 数据库层用 unique_observation_species 唯一键保证同一次观测不会重复关联同一物种。
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "observation_species",
        uniqueConstraints = @UniqueConstraint(name = "unique_observation_species",
                columnNames = {"observation_id", "species_id"}))
public class ObservationSpecies extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id", nullable = false)
    private Observation observation;

    /** 关联项几乎总是要连带展示物种名与保护等级，因此一次性取出，避免脱离事务后再访问报懒加载异常 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    /** 估算数量 */
    private Integer count;

    /** 观察到的行为，如「摄食」「繁殖」 */
    @Column(length = 255)
    private String behavior;

    @Column(length = 500)
    private String note;

    
}
