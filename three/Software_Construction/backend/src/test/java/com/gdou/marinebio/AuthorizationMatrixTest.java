package com.gdou.marinebio;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.config.SecurityConfig;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.OperationLogRepository;
import com.gdou.marinebio.repository.UserRepository;
import com.gdou.marinebio.service.AiService;
import com.gdou.marinebio.service.EcosystemService;
import com.gdou.marinebio.service.LogService;
import com.gdou.marinebio.service.ObservationService;
import com.gdou.marinebio.service.SpeciesService;
import com.gdou.marinebio.service.StatsService;
import com.gdou.marinebio.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * 权限矩阵（报告 表 3-2）的可执行版本。
 *
 * <p>为什么单独写这一个测试：{@code SecurityConfig} 里的规则是「先注册先生效」的顺序敏感列表，
 * 一次改动很容易把某一条挤到别的通配规则后面，而这类越权在编译期和冒烟测试里都看不出来
 * ——SmokeTest 全程用 admin 登录，admin 对所有 URL 都是放行的，一条「公众也能进看板」这种
 * 回归它永远测不到。这里把四个角色 × 各个接口写成断言，规则一旦被打乱就会红。
 *
 * <p>用 {@code @WebMvcTest} 而不是 {@code @SpringBootTest}：后者要拉数据源并做 JPA schema 校验，
 * 本机 MySQL 没起就跑不了。这里只装载 Web 切片 + 真实的 SecurityConfig 过滤器链，
 * 业务 Bean 全部换成 mock，因此测的正是「这个 URL + 这个方法 + 这个角色，
 * AuthorizationFilter 放不放行」这一段真实逻辑，不涉及业务返回值。
 *
 * <p>断言只分两类：
 * <ul>
 *   <li>{@code denied} —— 必须是 401（未登录）或 403（角色不够）；</li>
 *   <li>{@code allowed} —— 只要<b>不是</b> 401/403 即可。业务 Bean 是 mock，请求放行之后
 *       控制器可能因为缺参数返回 400 或因为 mock 返回 null 得到 200，
 *       这些都说明鉴权放行了，不该算失败。</li>
 * </ul>
 */
@WebMvcTest
@Import(SecurityConfig.class)
class AuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    /** SecurityConfig 装配出来的真实认证链：认证相关的断言要跑真 provider，不能自己拼一份 */
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private OperationLogRepository operationLogRepository;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private SpeciesService speciesService;
    @MockitoBean
    private ObservationService observationService;
    @MockitoBean
    private EcosystemService ecosystemService;
    @MockitoBean
    private StatsService statsService;
    @MockitoBean
    private LogService logService;
    @MockitoBean
    private AiService aiService;

    /**
     * SessionFreshnessFilter 排在鉴权之前，每个已登录请求都会按 id 回查一次数据库并比对
     * 角色 / 状态 / 凭据版本号。这里让库里读出来的就是同一个对象，会话才会被判为「仍然新鲜」，
     * 从而把测试焦点留在 URL 鉴权上，而不是被这个过滤器先拦成 401。
     */
    @BeforeEach
    void letSessionThrough() {
        when(userRepository.findById(anyInt())).thenAnswer(call ->
                Optional.of(user(call.getArgument(0), Role.ADMIN)));
    }

    private static User user(Integer id, Role role) {
        return user(id, "u" + id, role, UserStatus.ACTIVE);
    }

    private static User user(Integer id, String username, Role role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName("测试用户");
        user.setPassword("whatever");
        user.setRole(role);
        user.setStatus(status);
        user.setCredentialVersion(1);
        return user;
    }

    private MockHttpServletRequestBuilder req(HttpMethod method, String url, Role role) {
        MockHttpServletRequestBuilder builder = request(method, url);
        if (role != null) {
            Integer id = 100 + role.ordinal();
            // findById 的桩要返回同一个角色的用户，否则 SessionFreshnessFilter 会判定角色已变更
            when(userRepository.findById(id)).thenReturn(Optional.of(user(id, role)));
            LoginUser principal = new LoginUser(user(id, role));
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            builder.with(authentication(auth));
        }
        // CsrfFilter 排在 AuthorizationFilter 之前：状态变更请求不带令牌会先被 CSRF 判成 403，
        // 那会伪装成「鉴权拦住了」，把越权漏放行测成通过。所以写操作一律补上有效令牌。
        if (method != HttpMethod.GET) {
            builder.with(csrf());
        }
        return builder;
    }

    private void assertStatus(HttpMethod method, String url, Role role, boolean expectDenied) throws Exception {
        var result = mockMvc.perform(req(method, url, role)).andReturn();
        int status = result.getResponse().getStatus();
        boolean denied = status == 401 || status == 403;
        assertThat(denied)
                .as("%s %s 角色=%s 期望%s，实际 HTTP %d", method, url,
                        role == null ? "匿名" : role.name(), expectDenied ? "拒绝" : "放行", status)
                .isEqualTo(expectDenied);
    }

    @Test
    @DisplayName("矩阵第 160-162 行：浏览公开物种信息与多条件检索，四个角色都含公众与匿名")
    void publicSpeciesReadsAreOpenToEveryone() throws Exception {
        for (Role role : new Role[]{null, Role.PUBLIC, Role.STUDENT, Role.RESEARCHER, Role.ADMIN}) {
            assertStatus(HttpMethod.GET, "/api/species", role, false);
            assertStatus(HttpMethod.GET, "/api/species/1", role, false);
            assertStatus(HttpMethod.GET, "/api/species/map", role, false);
            assertStatus(HttpMethod.GET, "/api/species/phylums", role, false);
            assertStatus(HttpMethod.GET, "/api/ecosystems", role, false);
            assertStatus(HttpMethod.GET, "/api/ecosystems/1", role, false);
        }
    }

    @Test
    @DisplayName("统计类接口：公众 ×，学生 / 科研人员 / 管理员 √（矩阵第 170-171 行）")
    void statsEndpointsFollowTheMatrix() throws Exception {
        assertStatus(HttpMethod.GET, "/api/stats/dashboard", null, true);
        assertStatus(HttpMethod.GET, "/api/stats/dashboard", Role.PUBLIC, true);
        assertStatus(HttpMethod.GET, "/api/stats/dashboard", Role.STUDENT, false);
        assertStatus(HttpMethod.GET, "/api/stats/dashboard", Role.RESEARCHER, false);
        assertStatus(HttpMethod.GET, "/api/stats/dashboard", Role.ADMIN, false);

        for (String url : new String[]{"/api/ecosystems/stats", "/api/observations/map"}) {
            assertStatus(HttpMethod.GET, url, Role.PUBLIC, true);
            assertStatus(HttpMethod.GET, url, Role.STUDENT, false);
            assertStatus(HttpMethod.GET, url, Role.ADMIN, false);
        }
    }

    @Test
    @DisplayName("模块五按能力细分：补全 √√××，标签与问答 √√√×，识别与翻译 √√√√")
    void aiEndpointsFollowTheMatrix() throws Exception {
        assertStatus(HttpMethod.POST, "/api/ai/complete", Role.PUBLIC, true);
        assertStatus(HttpMethod.POST, "/api/ai/complete", Role.STUDENT, true);
        assertStatus(HttpMethod.POST, "/api/ai/complete", Role.RESEARCHER, false);
        assertStatus(HttpMethod.POST, "/api/ai/complete", Role.ADMIN, false);

        for (String url : new String[]{"/api/ai/analyze", "/api/ai/ask", "/api/ai/history"}) {
            assertStatus(HttpMethod.POST, url, Role.PUBLIC, true);
            assertStatus(HttpMethod.POST, url, Role.STUDENT, false);
            assertStatus(HttpMethod.POST, url, Role.ADMIN, false);
        }
        assertStatus(HttpMethod.DELETE, "/api/ai/history", Role.PUBLIC, true);
        assertStatus(HttpMethod.DELETE, "/api/ai/history", Role.STUDENT, false);

        // 识别 / 翻译 / 调用记录对公众也开放，公众唯一的入口就是这两个
        for (String url : new String[]{"/api/ai/identify", "/api/ai/translate", "/api/ai/records"}) {
            assertStatus(HttpMethod.POST, url, Role.PUBLIC, false);
            assertStatus(HttpMethod.GET, url, Role.PUBLIC, false);
        }
        assertStatus(HttpMethod.POST, "/api/ai/identify", null, true);
    }

    @Test
    @DisplayName("图像识别的前置步骤：公众与学生必须能上传，否则矩阵里那行「√√√√」是空的")
    void uploadIsReachableByEveryLoggedInRole() throws Exception {
        // 故意用普通 POST 而不是 multipart 请求：鉴权发生在参数绑定之前，
        // AuthorizationFilter 决定放不放行时根本还没碰过 MultipartFile。
        // 用 multipart() 构造时这条断言会失去灵敏度——把上面那条规则改回
        // hasAnyRole("ADMIN","RESEARCHER") 也测不出来，所以规则本身要走 request() 这条路断言。
        for (Role role : new Role[]{Role.PUBLIC, Role.STUDENT, Role.RESEARCHER, Role.ADMIN}) {
            assertStatus(HttpMethod.POST, "/api/upload", role, false);
        }
        assertStatus(HttpMethod.POST, "/api/upload", null, true);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticationFor(Role role) {
        Integer id = 100 + role.ordinal();
        when(userRepository.findById(id)).thenReturn(Optional.of(user(id, role)));
        LoginUser principal = new LoginUser(user(id, role));
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("写入接口：物种 / 生态系统 / 观测的增改限科研人员与管理员，删除限管理员（矩阵 163-169）")
    void writeEndpointsAreRestrictedToEditors() throws Exception {
        assertStatus(HttpMethod.POST, "/api/species", Role.PUBLIC, true);
        assertStatus(HttpMethod.POST, "/api/species", Role.STUDENT, true);
        assertStatus(HttpMethod.POST, "/api/species", Role.RESEARCHER, false);
        assertStatus(HttpMethod.POST, "/api/species", null, true);

        assertStatus(HttpMethod.PUT, "/api/species/1", Role.STUDENT, true);
        assertStatus(HttpMethod.PUT, "/api/species/1", Role.RESEARCHER, false);

        // 删除物种、删除生态系统：矩阵只有管理员一列是 √
        assertStatus(HttpMethod.DELETE, "/api/species/1", Role.RESEARCHER, true);
        assertStatus(HttpMethod.DELETE, "/api/ecosystems/1", Role.RESEARCHER, true);
        assertStatus(HttpMethod.DELETE, "/api/species/1", Role.ADMIN, false);

        // 观测记录的 URL 规则放行到科研人员，「限本人」由业务层再判
        assertStatus(HttpMethod.DELETE, "/api/observations/1", Role.STUDENT, true);
        assertStatus(HttpMethod.DELETE, "/api/observations/1", Role.RESEARCHER, false);
        assertStatus(HttpMethod.DELETE, "/api/observations/1", Role.ADMIN, false);
    }

    @Test
    @DisplayName("管理员专属：审核、改角色、重置他人密码、用户列表、操作日志（矩阵 178-180）")
    void adminOnlyEndpoints() throws Exception {
        for (Role role : new Role[]{Role.PUBLIC, Role.STUDENT, Role.RESEARCHER}) {
            assertStatus(HttpMethod.GET, "/api/users", role, true);
            assertStatus(HttpMethod.GET, "/api/users/1", role, true);
            assertStatus(HttpMethod.GET, "/api/users/overview", role, true);
            assertStatus(HttpMethod.POST, "/api/users/1/approve", role, true);
            assertStatus(HttpMethod.PUT, "/api/users/1/role", role, true);
            assertStatus(HttpMethod.PUT, "/api/users/1/password", role, true);
            assertStatus(HttpMethod.GET, "/api/logs", role, true);
        }
        assertStatus(HttpMethod.GET, "/api/users", Role.ADMIN, false);
        assertStatus(HttpMethod.POST, "/api/users/1/approve", Role.ADMIN, false);
        assertStatus(HttpMethod.PUT, "/api/users/1/role", Role.ADMIN, false);
        assertStatus(HttpMethod.GET, "/api/logs", Role.ADMIN, false);
    }

    @Test
    @DisplayName("本人资料与本人改密：任何登录角色都能用，必须排在 /api/users/** 管理员规则之前")
    void selfServiceEndpointsStayOpenToAllRoles() throws Exception {
        assertStatus(HttpMethod.PUT, "/api/users/profile", null, true);
        for (Role role : new Role[]{Role.PUBLIC, Role.STUDENT, Role.RESEARCHER, Role.ADMIN}) {
            assertStatus(HttpMethod.PUT, "/api/users/profile", role, false);
            assertStatus(HttpMethod.POST, "/api/users/password", role, false);
        }
    }

    @Test
    @DisplayName("观测记录的读取：已登录即可（报告 表 8-5），匿名拒绝")
    void observationReads() throws Exception {
        assertStatus(HttpMethod.GET, "/api/observations", null, true);
        assertStatus(HttpMethod.GET, "/api/observations/1", null, true);
        for (Role role : new Role[]{Role.PUBLIC, Role.STUDENT, Role.RESEARCHER, Role.ADMIN}) {
            assertStatus(HttpMethod.GET, "/api/observations", role, false);
            assertStatus(HttpMethod.GET, "/api/observations/1", role, false);
        }
    }

    @Test
    @DisplayName("认证接口与前端静态资源：不能被鉴权规则误伤")
    void publicAuthAndStaticStayOpen() throws Exception {
        // 只有 POST 的 login/register 与 GET 的 csrf 是 permitAll；
        // 反过来请求 GET /api/auth/login 落到兜底的 /api/** 上应当是 401，那才是对的。
        assertStatus(HttpMethod.POST, "/api/auth/login", null, false);
        assertStatus(HttpMethod.POST, "/api/auth/register", null, false);
        assertStatus(HttpMethod.GET, "/api/auth/csrf", null, false);
        assertStatus(HttpMethod.GET, "/api/auth/login", null, true);

        assertStatus(HttpMethod.GET, "/", null, false);
        assertStatus(HttpMethod.GET, "/species", null, false);
        assertStatus(HttpMethod.GET, "/assets/index-abc123.js", null, false);
    }

    @Test
    @DisplayName("/api/auth/me 在已登录时放行，供前端刷新后确认身份")
    void meIsOpenToLoggedInUsers() throws Exception {
        assertStatus(HttpMethod.GET, "/api/auth/me", Role.PUBLIC, false);
        assertStatus(HttpMethod.GET, "/api/auth/me", Role.ADMIN, false);
    }

    // ===== 认证：不给匿名者留账号枚举的口 =====

    @Test
    @DisplayName("待审核账号 + 错误口令 = 「用户名或密码错误」，不能泄露「该账号正在待审核」")
    void wrongPasswordNeverRevealsPendingState() {
        User pending = user(50, "pending", Role.STUDENT, UserStatus.PENDING);
        pending.setPassword(passwordEncoder.encode("right-password"));
        when(userRepository.findByUsername("pending")).thenReturn(Optional.of(pending));

        // 状态检查必须在口令校验之后。原先 UserDetailsService 读到人就抛 DisabledException，
        // 口令还没比对就已经回了「账号尚未通过审核」——那等于一个不需要口令的用户名探测接口。
        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("pending", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("待审核账号即使口令正确也登不进去，并给出可操作的提示")
    void pendingAccountCannotLoginWithCorrectPassword() {
        User pending = user(51, "pending", Role.STUDENT, UserStatus.PENDING);
        pending.setPassword(passwordEncoder.encode("right-password"));
        when(userRepository.findByUsername("pending")).thenReturn(Optional.of(pending));

        // 拦截仍然要成立，只是理由现在只在口令正确时才说得出
        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("pending", "right-password")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("不存在的用户名与密码错误回同一种异常，不做区分")
    void unknownUserIsIndistinguishableFromWrongPassword() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("nobody", "whatever")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("被驳回的账号同样不能登录，而且同样是口令先过、状态后判")
    void rejectedAccountIsAlsoGatedAfterPasswordCheck() {
        User rejected = user(53, "rejected", Role.STUDENT, UserStatus.REJECTED);
        rejected.setPassword(passwordEncoder.encode("right-password"));
        when(userRepository.findByUsername("rejected")).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("rejected", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        assertThatThrownBy(() -> authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("rejected", "right-password")))
                .isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("在岗账号 + 正确口令仍然可以正常登录")
    void activeUserWithCorrectPasswordAuthenticates() {
        User active = user(52, "researcher", Role.RESEARCHER, UserStatus.ACTIVE);
        active.setPassword(passwordEncoder.encode("right-password"));
        when(userRepository.findByUsername("researcher")).thenReturn(Optional.of(active));

        Authentication result = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken("researcher", "right-password"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getPrincipal()).isInstanceOf(LoginUser.class);
        assertThat(((LoginUser) result.getPrincipal()).getRole()).isEqualTo(Role.RESEARCHER);
    }

    @Test
    @DisplayName("未登录被拒要回 401 而不是 403，否则前端不会跳登录页")
    void anonymousWriteIsRejectedWithUnauthorizedNotForbidden() throws Exception {
        // 匿名 POST 会先被 CsrfFilter 拦下并抛 AccessDeniedException，
        // 原先统一回 403。而 request.js 只对 401 做「清状态 + 跳登录」，
        // 于是会话刚过期就点保存的用户只会看到一句权限不足，停在原地刷不出登录框。
        var anon = mockMvc.perform(request(HttpMethod.POST, "/api/species").with(csrf())
                        .contentType("application/json").content("{}"))
                .andReturn();
        assertThat(anon.getResponse().getStatus()).isEqualTo(401);
        assertThat(anon.getResponse().getContentAsString()).contains("重新登录");

        // 已登录但角色不够，仍然是 403：他不该被踢下线
        var forbidden = mockMvc.perform(req(HttpMethod.GET, "/api/stats/dashboard", Role.PUBLIC))
                .andReturn();
        assertThat(forbidden.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("已登录但 CSRF 令牌失效要回 403 并提示刷新，不能被踢下线")
    void staleCsrfTokenDoesNotLogOutAnAuthenticatedUser() throws Exception {
        // 真机上复现过：CsrfFilter 抛出的 AccessDeniedException 由 ExceptionTranslationFilter 处理，
        // 此时容器级 request.getUserPrincipal() 还是空的（Spring Security 不去设 Tomcat 的 principal），
        // 于是 handler 判成「未登录」回了 401；前端 request.js 一见 401 就 clear() 本地状态并跳登录页，
        // 用户填了很久的表单直接作废。判据换成 SecurityContextHolder 后：已登录 403 + 提示刷新，
        // 匿名仍是 401（上一条用例锁着）。
        // 注意 MockMvc 的 authentication() 会顺手把 request.userPrincipal 设好，
        // 所以这条在改造前并不会失败——它是靠真机 HTTP 才暴露出来的。
        var result = mockMvc.perform(request(HttpMethod.POST, "/api/species")
                        .with(authenticationFor(Role.ADMIN))
                        .contentType("application/json").content("{}"))
                .andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getContentAsString()).contains("刷新页面");
    }

    @Test
    @DisplayName("写错的接口路径不能冒充成功响应")
    void unknownApiPathIsNotAnOkayResponse() throws Exception {
        // 兜底的 /api/** 规则排在鉴权里，所以匿名拿到 401——鉴权先于 handler 解析，
        // 这条路径根本不会被当成「接口存在但没数据」。
        var anonymous = mockMvc.perform(get("/api/does-not-exist")).andReturn();
        assertThat(anonymous.getResponse().getStatus()).isEqualTo(401);

        // 已登录时才是 404。WebConfig 的 SPA 兜底刻意排除了 api/ 前缀：
        // 写错的接口名如果被 index.html 顶替，客户端会收到 200 + 一坨 HTML，
        // 排查时完全看不出是路径打错了。
        var loggedIn = mockMvc.perform(get("/api/does-not-exist")
                .with(authenticationFor(Role.ADMIN))).andReturn();
        assertThat(loggedIn.getResponse().getStatus()).isEqualTo(404);
        assertThat(loggedIn.getResponse().getContentAsString()).doesNotContain("<div id=\"app\">");
    }
}
