package com.gdou.marinebio.common;

import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import lombok.Getter;
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
    /**
     * 登录那一刻的凭据版本号与账号状态快照。会话本身活 2 小时，
     * 期间用户改密码、被降权、被停用都不会反映到已经持有的会话上，
     * 所以 SessionFreshnessFilter 每次请求都要拿它和库里的当前值比一次。
     */
    private final Integer credentialVersion;
    private final UserStatus status;

    public LoginUser(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.realName = user.getRealName();
        this.role = user.getRole();
        this.credentialVersion = user.getCredentialVersion();
        this.status = user.getStatus();
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

    /**
     * 只有审核通过（ACTIVE）的账号才算「可用」。
     *
     * <p>Spring Security 6 默认把 isEnabled() 放在 pre-checks，也就是口令校验**之前**，
     * 直接依赖默认行为会让匿名调用者靠回显差异枚举出存在的用户名。
     * 本项目的 {@code SecurityConfig#authenticationProvider} 因此清空了 pre-checks
     * 并把这道判断移到 post-checks，让「状态不可用」只在口令正确时才说得出。
     * 改这里之前请先看那边的注释：两步缺一不可。
     */
    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }

    // 未登录取字段的 null 安全入口只有下面这一个（@AuthenticationPrincipal LoginUser 场景，
    // permitAll 接口拿到的 principal 是 null）；SpeciesController 4 个公开接口在用。

    public static Role roleOf(LoginUser user) {
        return user == null ? null : user.getRole();
    }
}
