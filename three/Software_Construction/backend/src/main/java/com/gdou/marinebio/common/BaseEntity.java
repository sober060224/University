package com.gdou.marinebio.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 各实体的创建时间。
 *
 * 建表语句里虽然写了 DEFAULT CURRENT_TIMESTAMP，但 Hibernate 插入时会把字段显式写成 NULL，
 * 数据库默认值根本不会触发，接口返回的创建时间就一直是 null。
 * 与其绕开这个行为，不如在实体层自己填，保证返回给前端的值一定是准的。
 *
 * <p>子类不要再声明同名的 createTime 字段：Java 的字段遮蔽会让 Lombok 在子类再生成一对
 * 读写方法，@PrePersist 填的是这里的字段，接口读到的却是子类的那个，结果永远是 null。
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (createTime == null) {
            createTime = now;
        }
    }
}
