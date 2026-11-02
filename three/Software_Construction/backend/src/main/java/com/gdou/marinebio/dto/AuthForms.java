package com.gdou.marinebio.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 模块一与模块五的请求体 */
public final class AuthForms {

    private AuthForms() {
    }

    /**
     * 登录。两个字段都必须给长度上限：这是系统上少数几个 permitAll 的接口之一，
     * 不限长的话一个请求体就能塞进几十 MB 的口令，白白烧掉一次 BCrypt 计算和一次
     * 请求体解析——而数据库里的 username 只有 VARCHAR(50)，超长必然写不进也读不出。
     */
    public record Login(
            @NotBlank(message = "请输入用户名")
            @Size(max = 50, message = "用户名长度不能超过 50 个字符") String username,
            @NotBlank(message = "请输入密码")
            @Size(max = 100, message = "密码长度不能超过 100 个字符") String password) {
    }

    /**
     * 注册申请。applyRole 只能申请 STUDENT 或 PUBLIC，
     * RESEARCHER 与 ADMIN 必须由管理员在审核时另行授予。
     *
     * <p>{@code @Size} 一律写中文 message：不写就走校验框架的默认值
     * 「个数必须在0和20之间」，既没说清是哪个字段，「个数」对字符串而言也不是长度；
     * 而 GlobalExceptionHandler 会把多个字段的消息用「；」拼成一句，
     * 只看到一串「个数必须在0和N之间」的用户无法知道该改哪一格。
     */
    public record Register(
            @NotBlank(message = "请输入用户名")
            @Size(min = 3, max = 50, message = "用户名长度需在 3 到 50 个字符之间") String username,
            @NotBlank(message = "请输入密码")
            @Size(min = 6, max = 50, message = "密码长度不能少于 6 位") String password,
            @NotBlank(message = "请输入姓名")
            @Size(max = 50, message = "姓名长度不能超过 50 个字符") String realName,
            @NotBlank(message = "请选择申请身份") String applyRole,
            @Size(max = 20, message = "手机号长度不能超过 20 个字符") String phone,
            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱长度不能超过 100 个字符") String email,
            @Size(max = 30, message = "学号长度不能超过 30 个字符") String studentNo) {
    }

    /**
     * 管理员审核：approved 决定激活还是驳回，role 用于在通过时一并授予科研人员身份。
     * approved 必须显式给出——审核是账号进入系统的唯一闸门，缺字段按「不通过」处理，
     * 绝不能让一个残缺的请求体把账号放行。
     */
    public record Approve(
            @NotNull(message = "请选择审核结果") Boolean approved,
            @Size(max = 20, message = "角色长度不能超过 20 个字符") String role) {
    }

    public record Profile(
            @Size(max = 50, message = "姓名长度不能超过 50 个字符") String realName,
            @Size(max = 10, message = "性别长度不能超过 10 个字符") String gender,
            @Size(max = 20, message = "手机号长度不能超过 20 个字符") String phone,
            @Email(message = "邮箱格式不正确")
            @Size(max = 100, message = "邮箱长度不能超过 100 个字符") String email,
            @Size(max = 255, message = "头像地址长度不能超过 255 个字符") String avatar,
            @Size(max = 30, message = "学号长度不能超过 30 个字符") String studentNo) {
    }

    public record ChangePassword(
            @NotBlank(message = "请输入原密码")
            @Size(max = 100, message = "原密码长度不能超过 100 个字符") String oldPassword,
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 50, message = "新密码长度不能少于 6 位") String newPassword) {
    }

    public record ResetPassword(
            @NotBlank(message = "请输入新密码")
            @Size(min = 6, max = 50, message = "新密码长度不能少于 6 位") String newPassword) {
    }

    public record AssignRole(
            @NotBlank(message = "请选择角色")
            @Size(max = 20, message = "角色长度不能超过 20 个字符") String role) {
    }

    /**
     * 模块五各能力的入参。
     * 长度上限与 ai_records 的列宽对齐（input_text VARCHAR(1000)、input_image VARCHAR(255)）：
     * 超长不会在这里被拦下，而是等 saveRecord 写库时才由 MySQL 抛「Data too long」，
     * 用户看到的是笼统的「服务器内部错误」，问题定位也变得困难。
     */

    /** 模块五：图像智能识别与物种鉴定 */
    public record Identify(
            @NotBlank(message = "请先上传图片")
            @Size(max = 255, message = "图片地址长度不能超过 255 个字符") String imageUrl) {
    }

    /** 模块五：文本辅助分类与补全 */
    public record Complete(
            @NotBlank(message = "请输入中文名或学名中的至少一项")
            @Size(max = 1000, message = "名称长度不能超过 1000 个字符") String name) {
    }

    /** 模块五：物种描述多语言翻译 */
    public record Translate(
            @NotBlank(message = "没有可翻译的内容")
            @Size(max = 1000, message = "待翻译内容长度不能超过 1000 个字符") String text,
            @NotBlank(message = "请选择目标语言")
            @Size(max = 50, message = "目标语言长度不能超过 50 个字符") String targetLanguage) {
    }

    /**
     * 模块五：观测记录智能标签与异常检测。
     * 直接传草稿字段而不是 observationId —— 新增观测时记录还没入库，
     * 分析完再连同分析结果一起提交。
     */
    public record Analyze(
            @Size(max = 30, message = "观测时间长度不能超过 30 个字符") String observeTime,
            @Size(max = 200, message = "观测地点长度不能超过 200 个字符") String locationName,
            // 草稿数据同样要限范围：analyze 会把坐标拼进提示词交给模型，
            // 一个 100000 的经度不会报错，只会让模型输出一段荒谬结果。
            @DecimalMin(value = "-180.0", message = "经度需在 -180 到 180 之间")
            @DecimalMax(value = "180.0", message = "经度需在 -180 到 180 之间") Double longitude,
            @DecimalMin(value = "-90.0", message = "纬度需在 -90 到 90 之间")
            @DecimalMax(value = "90.0", message = "纬度需在 -90 到 90 之间") Double latitude,
            @Size(max = 100, message = "生态系统名称长度不能超过 100 个字符") String ecosystemName,
            // 每个物种名都会触发一次 matchByName 查询，不设上限等于让一个请求
            // 驱动任意多次数据库往返。
            // 元素上既要限长度也要禁 null：直接拼进提示词时一个 null 就是字符串 "null"。
            @Size(max = 50, message = "一次分析最多支持 50 个物种")
            List<@NotBlank(message = "物种名不能为空")
                 @Size(max = 150, message = "单个物种名长度不能超过 150 个字符") String> speciesNames) {
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
