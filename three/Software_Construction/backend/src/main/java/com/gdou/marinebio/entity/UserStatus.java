package com.gdou.marinebio.entity;

/**
 * 账号状态。注册后需管理员审核才可登录，对应模块一「用户注册（学生/公众可申请，需审核）」
 */
public enum UserStatus {
    /** 待审核 */
    PENDING,
    /** 已通过，可用 */
    ACTIVE,
    /** 已拒绝 */
    REJECTED
}
