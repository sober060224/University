package com.gdou.marinebio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 模块一与模块五的请求体 */
public final class AuthForms {

    private AuthForms() {
    }

    public record Login(
            @NotBlank(message = "请输入用户名") String username,
            @NotBlank(message = "请输入密码") String password) {
    }

    /**
     * 注册申请。applyRole 只能申请 STUDENT 或 PUBLIC，
     * RESEARCHER 与 ADMIN 必须由管理员在审核时另行授予。
     */
    public record Register(
            @NotBlank(message = "请输入用户名")
            @Size(min = 3, max = 50, message = "用户名长度需在 3 到 50 个字符之间") String username,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 50, message = "密码长度不能少于 6 位") String password,
            @NotBlank(message = "请输入姓名") @Size(max = 50) String realName,
            @NotBlank(message = "请选择申请身份") String applyRole,
            @Size(max = 20) String phone,
            @Email(message = "邮箱格式不正确") @Size(max = 100) String email,
            @Size(max = 30) String studentNo) {
    }

    /** 管理员审核：approved 决定激活还是驳回，role 用于在通过时一并授予科研人员身份 */
    public record Approve(
            Boolean approved,
            String role) {
    }

    public record Profile(
            @Size(max = 50) String realName,
            @Size(max = 10) String gender,
            @Size(max = 20) String phone,
            @Email(message = "邮箱格式不正确") @Size(max = 100) String email,
            @Size(max = 255) String avatar,
            @Size(max = 30) String studentNo) {
    }

    public record ChangePassword(
            @NotBlank(message = "请输入原密码") String oldPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 50, message = "新密码长度不能少于 6 位") String newPassword) {
    }

    public record ResetPassword(
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 50, message = "新密码长度不能少于 6 位") String newPassword) {
    }

    public record AssignRole(
            @NotBlank(message = "请选择角色") String role) {
    }

    /** 模块五：图像智能识别与物种鉴定 */
    public record Identify(
            @NotBlank(message = "请先上传图片") String imageUrl) {
    }

    /** 模块五：文本辅助分类与补全 */
    public record Complete(
            @NotBlank(message = "请输入中文名或学名中的至少一项") String name) {
    }

    /** 模块五：物种描述多语言翻译 */
    public record Translate(
            @NotBlank(message = "没有可翻译的内容") String text,
            @NotBlank(message = "请选择目标语言") String targetLanguage) {
    }

    /**
     * 模块五：观测记录智能标签与异常检测。
     * 直接传草稿字段而不是 observationId —— 新增观测时记录还没入库，
     * 分析完再连同分析结果一起提交。
     */
    public record Analyze(
            String observeTime,
            String locationName,
            Double longitude,
            Double latitude,
            String ecosystemName,
            List<String> speciesNames) {
    }

    /** 模块五：智能问答与科研助手 */
    public record Ask(
            @NotBlank(message = "请输入你的问题") String question) {
    }
}
