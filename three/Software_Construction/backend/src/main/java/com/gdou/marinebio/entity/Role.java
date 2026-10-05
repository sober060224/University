package com.gdou.marinebio.entity;

/**
 * 用户角色，对应愿景文档 3.1 模块一「用户角色管理（管理员、科研人员、学生、公众）」
 */
public enum Role {
    /** 系统管理员：用户审核、角色管理、系统维护 */
    ADMIN,
    /** 科研人员/教师：核心用户，录入维护分析物种与观测数据 */
    RESEARCHER,
    /** 学生：查询学习，参与课程实践 */
    STUDENT,
    /** 公众：浏览公开物种信息，进行科普学习 */
    PUBLIC
}
