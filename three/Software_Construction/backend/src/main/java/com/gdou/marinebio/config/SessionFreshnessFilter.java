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
import org.springframework.dao.DataAccessException;
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
 *
 * <p>每个已登录请求多一次 findById 主键查询是设计代价而不是缺陷：这个过滤器的
 * 全部意义就是「停用/降级/改密之后旧会话下一请求立即失效」，加一层缓存等于
 * 把失效延迟换成缓存 TTL，安全语义就没了。主键命中在毫秒级；若登录用户量
 * 大到这条查询成为瓶颈，应改成长连接失效事件（改密时 bump 版本号已经做了，
 * 只是这里需要主动推），而不是牺牲即时失效。
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

        User current;
        try {
            current = userRepository.findById(loginUser.getId()).orElse(null);
        } catch (DataAccessException e) {
            // 数据库此刻不可达（连接池耗尽、网络抖动），不代表这个账号有问题。
            // 此时只有两条路：把请求放行等于「查不到状态就当有效」，
            // 停用/降级中的账号会趁这个空窗继续用——那是安全漏洞；
            // 直接注销会话等于一次数据库抖动就把所有人踢下线。
            // 取中间值：请求本身失败（503，前端会重试），但会话保留，
            // 数据库恢复后同一个会话继续按真实状态判定。
            log.warn("查询会话状态失败，本次请求拒绝但会话保留：{}", e.getMessage());
            writeJson(response, 503, false, "服务暂时不可用，请稍后重试");
            return;
        }
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
        // 与 SecurityConfig.writeJson 一样先把双引号与反斜杠转掉：提示语将来若拼进用户名，
        // 一个引号就能把响应体拆坏。
        String safe = message.replace("\\", "\\\\").replace("\"", "\\\"");
        res.getWriter().write("{\"success\":" + success + ",\"message\":\"" + safe + "\",\"data\":null}");
    }
}
