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
 * 生态系统，对应模块三「生态系统管理：珊瑚礁、红树林、海草床、深海等」
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "ecosystems")
public class Ecosystem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 30)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 255)
    private String imageUrl;
}
