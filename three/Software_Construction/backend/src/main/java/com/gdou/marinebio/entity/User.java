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
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;



/**
 * 用户账号，对应模块一「用户与权限管理」
 */
@Getter
@Setter
@ToString
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * BCrypt 加密后的密文，绝不返回给前端。
     * 实体本身会被若干 Controller 直接序列化，所以靠 JsonIgnore 在字段上兜住，
     * 而不是依赖每个 Service 都记得换成一个不含密码的 DTO。
     */
    @JsonIgnore
    @ToString.Exclude
    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "real_name", nullable = false, length = 50)
    private String realName;

    @Column(length = 10)
    private String gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 255)
    private String avatar;

    @Column(name = "student_no", length = 30)
    private String studentNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.PENDING;

    /**
     * 凭据版本号。改密码、重置密码、调整角色、账号被停用时自增，
     * 已登录会话里存的是登录那一刻的版本号，由 SessionFreshnessFilter 每次请求比对。
     * 不做这件事的话，密码改了、权限降了，对方手上的会话还能继续用满 2 小时。
     */
    @Column(name = "credential_version", nullable = false)
    private Integer credentialVersion = 1;
}
