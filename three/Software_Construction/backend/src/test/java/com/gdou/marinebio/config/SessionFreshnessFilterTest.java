package com.gdou.marinebio.config;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 会话新鲜度过滤器的失败分支。
 *
 * <p>要钉住的是「数据库此刻不可达」时的处理：这时既查不到账号状态，也无从判断
 * 该不该作废会话。放行等于「查不到就当有效」——正在被停用或降级的账号会趁这个
 * 空窗继续用；直接注销会话等于一次数据库抖动把所有人踢下线。正确的中间值是
 * 请求本身失败（503）、但会话保留，数据库恢复后同一个会话继续按真实状态判定。
 */
class SessionFreshnessFilterTest {

    private final UserRepository userRepository = mock(UserRepository.class);

    /** 构造一个登录主体：只带过滤器要比较的那几个字段 */
    private static LoginUser loginUser(Integer id, String username, Role role, UserStatus status) {
        var user = new com.gdou.marinebio.entity.User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setPassword("hash");
        user.setRole(role);
        user.setStatus(status);
        user.setCredentialVersion(1);
        return new LoginUser(user);
    }

    @Test
    @DisplayName("数据库不可达时返回 503，但会话不被注销")
    void databaseUnreachableKeepsTheSessionButFailsTheRequest() throws Exception {
        when(userRepository.findById(anyInt())).thenThrow(new QueryTimeoutException("连接超时"));

        var filter = new SessionFreshnessFilter(userRepository);
        var request = new MockHttpServletRequest("GET", "/api/species");
        var response = new MockHttpServletResponse();
        var chain = new RecordingChain();

        var principal = loginUser(3, "researcher", Role.RESEARCHER, UserStatus.ACTIVE);
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                AuthorityUtils.createAuthorityList("ROLE_RESEARCHER"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession(true);

        try {
            filter.doFilter(request, response, chain);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(chain.called)
                .as("请求不该被放行到下游")
                .isFalse();
        assertThat(response.getStatus())
                .as("数据库不可达是服务端问题，应回 503 而不是 401")
                .isEqualTo(503);
        assertThat(response.getContentAsString())
                .as("响应体要是前端能读懂的 JSON")
                .contains("\"success\":false")
                .contains("服务暂时不可用");
        assertThat(request.getSession(false))
                .as("会话必须保留：注销等于一次数据库抖动把所有人踢下线")
                .isNotNull();
    }

    @Test
    @DisplayName("账号确实被停用时才注销会话并回 401")
    void disabledAccountEndsTheSession() throws Exception {
        var disabled = new com.gdou.marinebio.entity.User();
        disabled.setId(3);
        disabled.setStatus(UserStatus.REJECTED);
        disabled.setRole(Role.RESEARCHER);
        disabled.setCredentialVersion(1);
        when(userRepository.findById(anyInt())).thenReturn(Optional.of(disabled));

        var filter = new SessionFreshnessFilter(userRepository);
        var request = new MockHttpServletRequest("GET", "/api/species");
        var response = new MockHttpServletResponse();
        var chain = new RecordingChain();

        var principal = loginUser(3, "researcher", Role.RESEARCHER, UserStatus.ACTIVE);
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                AuthorityUtils.createAuthorityList("ROLE_RESEARCHER"));
        SecurityContextHolder.getContext().setAuthentication(auth);
        request.getSession(true);

        try {
            filter.doFilter(request, response, chain);
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertThat(chain.called).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("账号已被停用");
    }

    /** 只记录「有没有被放行到下游」的过滤器链 */
    private static final class RecordingChain implements jakarta.servlet.FilterChain {
        private boolean called;

        @Override
        public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                throws IOException, jakarta.servlet.ServletException {
            called = true;
        }
    }
}
