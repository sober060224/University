package com.gdou.marinebio.common;

import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录主体：在 Spring Security 的 UserDetails 里额外带上用户 id 与角色枚举，
 * 让控制器和业务层直接取用，不必为了一个 id 再回查数据库。
 */
@Getter
public class LoginUser implements UserDetails {

    private final Integer id;
    private final String username;
    private final String password;
    private final String realName;
    private final Role role;

    public LoginUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.role = user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    /** 从当前认证上下文取用户 id；未登录返回 null */
    public static Integer idOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser.getId() : null;
    }

    public static String usernameOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser.getUsername() : null;
    }

    public static Role roleOf(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser.getRole() : null;
    }

    // ===== 下面三个是给 @AuthenticationPrincipal LoginUser 场景用的重载 =====
    // 允许匿名访问的接口拿到的 principal 是 null，直接取字段即可

    public static Integer idOf(LoginUser user) {
        return user == null ? null : user.getId();
    }

    public static String usernameOf(LoginUser user) {
        return user == null ? null : user.getUsername();
    }

    public static Role roleOf(LoginUser user) {
        return user == null ? null : user.getRole();
    }
}
