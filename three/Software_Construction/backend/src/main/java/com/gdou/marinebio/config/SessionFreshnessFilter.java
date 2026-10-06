package com.gdou.marinebio.config;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * 会话新鲜度校验：每个已登录请求都比对一次「会话里的凭据版本号 / 角色 / 状态」
 * 与「数据库里的当前值」。
 *
 * <p>为什么需要它：会话活 2 小时（server.servlet.session.timeout），
 * 而 SecurityContext 里存的是 {@link LoginUser} 登录那一刻的快照。只在
 * {@code userDetailsService} 里检查账号状态，等于只在登录那一刻检查一次——
 * 管理员停用一个账号、把科研人员降级成学生之后，对方手上的会话仍然畅通无阻，
 * 直到自然过期。改密码同样如此：受害者以为已经止损，实际没有。
 *
 * <p>绕开未登录请求：匿名浏览公开物种/生态系统的 permitAll 接口不产生任何
 * 数据库查询，这是系统的默认访问路径，不能给它加一次往返。
 */
@Slf4j
@RequiredArgsConstructor
public class SessionFreshnessFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 匿名请求、或 principal 不是 LoginUser（登录前）时直接放行
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        User current = userRepository.findById(loginUser.getId()).orElse(null);
        String reason = evaluate(loginUser, current);
        if (reason == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("会话已失效：user={} reason={}", loginUser.getUsername(), reason);
        // 走 LogoutHandler 而不是只清 SecurityContext：会话 id 本身也要作废，
        // 否则持有同一个 JSESSIONID 的请求还能继续复用它。
        new SecurityContextLogoutHandler().logout(request, response, auth);
        writeJson(response, 401, false, reason);
    }

    /** 会话仍然有效返回 null，否则返回作废理由（直接作为给前端的提示语） */
    private String evaluate(LoginUser session, User current) {
        if (current == null) {
            return "账号不存在，请重新登录";
        }
        if (current.getStatus() != UserStatus.ACTIVE) {
            return "账号已被停用，请联系管理员";
        }
        if (!Objects.equals(current.getCredentialVersion(), session.getCredentialVersion())) {
            return "登录信息已更新，请重新登录";
        }
        if (current.getRole() != session.getRole()) {
            return "账号权限已变更，请重新登录";
        }
        return null;
    }

    /** 与 SecurityConfig.writeJson 保持同一种结构，过滤器短路时前端也读得懂 */
    private void writeJson(HttpServletResponse res, int status, boolean success, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"success\":" + success + ",\"message\":\"" + message + "\",\"data\":null}");
    }
}
