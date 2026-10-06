package com.gdou.marinebio.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /**
     * 管理员审核：approved 决定激活还是驳回，role 用于在通过时一并授予科研人员身份。
     * approved 必须显式给出——审核是账号进入系统的唯一闸门，缺字段按「不通过」处理，
     * 绝不能让一个残缺的请求体把账号放行。
     */
    public record Approve(
            @NotNull(message = "请选择审核结果") Boolean approved,
            @Size(max = 20) String role) {
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

    /**
     * 模块五各能力的入参。
     * 长度上限与 ai_records 的列宽对齐（input_text VARCHAR(1000)、input_image VARCHAR(255)）：
     * 超长不会在这里被拦下，而是等 saveRecord 写库时才由 MySQL 抛「Data too long」，
     * 用户看到的是笼统的「服务器内部错误」，问题定位也变得困难。
     */

    /** 模块五：图像智能识别与物种鉴定 */
    public record Identify(
            @NotBlank(message = "请先上传图片") @Size(max = 255) String imageUrl) {
    }

    /** 模块五：文本辅助分类与补全 */
    public record Complete(
            @NotBlank(message = "请输入中文名或学名中的至少一项") @Size(max = 1000) String name) {
    }

    /** 模块五：物种描述多语言翻译 */
    public record Translate(
            @NotBlank(message = "没有可翻译的内容") @Size(max = 1000) String text,
            @NotBlank(message = "请选择目标语言") @Size(max = 50) String targetLanguage) {
    }

    /**
     * 模块五：观测记录智能标签与异常检测。
     * 直接传草稿字段而不是 observationId —— 新增观测时记录还没入库，
     * 分析完再连同分析结果一起提交。
     */
    public record Analyze(
            @Size(max = 30) String observeTime,
            @Size(max = 200) String locationName,
            Double longitude,
            Double latitude,
            @Size(max = 100) String ecosystemName,
            // 每个物种名都会触发一次 matchByName 查询，不设上限等于让一个请求
            // 驱动任意多次数据库往返。
            @Size(max = 50, message = "一次分析最多支持 50 个物种") List<@Size(max = 150) String> speciesNames) {
    }

    /** 模块五：智能问答与科研助手 */
    public record Ask(
            // 上限 1000 对应 ai_records.input_text；ask 是 @Transactional，
            // 超长导致写库失败时事务已被标记为 rollback-only，提交时会变成
            // UnexpectedRollbackException，白白浪费一次大模型调用还丢掉这条问答。
            @NotBlank(message = "请输入你的问题")
            @Size(max = 1000, message = "问题长度不能超过 1000 个字符") String question) {
    }
}
