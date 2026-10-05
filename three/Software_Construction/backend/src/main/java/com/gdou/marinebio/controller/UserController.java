package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 模块一：用户与权限管理。
 * 列表、审核、改角色、重置密码只对管理员开放（由 SecurityConfig 拦截），
 * 个人资料与改密码任何登录用户都能用。
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ========== 管理员功能 ==========

    @GetMapping
    public Result<PageResult<User>> list(@RequestParam(required = false) UserStatus status,
                                         @RequestParam(required = false) Role role,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageResult.of(page, size, 100, Sort.by(Sort.Direction.DESC, "id"));
        return Result.ok(userService.search(status, role, keyword, pageable));
    }

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(userService.overview());
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Integer id) {
        return Result.ok(userService.get(id));
    }

    @PostMapping("/{id}/approve")
    public Result<User> approve(@PathVariable Integer id,
                                @RequestBody AuthForms.Approve form,
                                @AuthenticationPrincipal LoginUser operator) {
        return Result.ok("审核完成", userService.approve(id, form, operator));
    }

    @PutMapping("/{id}/role")
    public Result<User> assignRole(@PathVariable Integer id,
                                   @RequestBody AuthForms.AssignRole form,
                                   @AuthenticationPrincipal LoginUser operator) {
        return Result.ok("角色已更新", userService.assignRole(id, form.role(), operator));
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Integer id,
                                      @RequestBody AuthForms.ResetPassword form,
                                      @AuthenticationPrincipal LoginUser operator) {
        userService.resetPassword(id, form.newPassword(), operator);
        return Result.ok("密码已重置", null);
    }

    // ========== 所有登录用户可用 ==========

    @PutMapping("/profile")
    public Result<User> updateProfile(@AuthenticationPrincipal LoginUser loginUser,
                                      @RequestBody AuthForms.Profile form) {
        return Result.ok("个人信息已更新", userService.updateProfile(loginUser.getId(), form));
    }

    @PostMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal LoginUser loginUser,
                                       @RequestBody AuthForms.ChangePassword form,
                                       HttpServletRequest request) {
        userService.changePassword(loginUser, form.oldPassword(), form.newPassword());
        // 改密后立刻作废当前会话：旧密码泄露出去时，攻击者手上的会话不能继续用
        request.getSession().invalidate();
        return Result.ok("密码已修改，请重新登录", null);
    }
}
