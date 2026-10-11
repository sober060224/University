package com.gdou.marinebio.config;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.io.IOException;

/**
 * 模块一：身份认证与基于角色的访问控制。
 *
 * 认证方式为 Session（单体部署够用，不必引入 JWT），密码 BCrypt 存储。
 * 权限规则与「角色—权限矩阵」一一对应。矩阵中的三种限制分别落在三处：
 * URL 规则（下面的 authorizeHttpRequests）、服务层行级过滤（按角色改写 is_public）、
 * 以及资源归属校验（观测记录只能改本人提交的）。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 矩阵里「查看统计报表与分布地图」「查看综合数据看板」对公众是 ×，对学生是 √ */
    private static final String[] STATS_ROLES = {"ADMIN", "RESEARCHER", "STUDENT"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 只负责把账号读出来并原样交给 Spring 的认证流程，**不在这lambda 里判审核状态**。
     *
     * <p>原来这里在读出用户后就地抛 DisabledException，而 DaoAuthenticationProvider 的
     * 顺序是「先取用户，后验密码」：等于密码还没比对就已经给出了「账号尚未通过审核」，
     * 任何未登录的人拿任意口令试 POST /api/auth/login，都能从回显的消息里区分出
     * 「这个用户名存在且正在待审核」和「用户名或密码错误」——一个不需要口令的账号枚举接口。
     *
     * <p>改成由 {@link LoginUser#isEnabled()} 反映状态之后，检查点落在
     * AbstractUserDetailsAuthenticationProvider 的 post-checks，即口令校验成功之后：
     * 提示语对合法用户依然清晰，但攻击者不掌握口令就只会得到统一的「用户名或密码错误」。
     * 顺带把另一个侧信道也关掉了：现在无论用户名是否存在，都会完整跑一次 BCrypt，
     * 响应时间不再暴露「这个账号不存在」。
     */
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(LoginUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    /**
     * 认证提供器：把「账号是否已审核通过」这道判断挪到口令校验**之后**。
     *
     * <p>AbstractUserDetailsAuthenticationProvider 的默认顺序是
     * 取用户 → preAuthenticationChecks（内含 isEnabled）→ 校验口令 → postAuthenticationChecks。
     * 也就是说 isEnabled() 默认就跑在比对口令之前，结果是任何匿名调用者都能靠
     * 「账号尚未通过审核」和「用户名或密码错误」两种回显，在不掌握口令的情况下
     * 枚举出哪些用户名存在且处于待审核状态——一个不需要口令的账号探测接口。
     *
     * <p>所以这里清空 pre-checks（本项目的 LoginUser 里 isAccountNonLocked /
     * isAccountNonExpired / isCredentialsNonExpired 恒定返回 true，本来就没有可检查的），
     * 再在 post-checks 里补回 isEnabled()——注意 Spring Security 6 把 isEnabled 归在
     * pre-checks 而 post-checks 只管凭证过期，所以「清空 pre + 补回 post」两步缺一不可：
     * 只清 pre 的话，待审核账号会直接登录成功（这一条正是测试抓出来的回归）。
     *
     * <p>顺带另一条侧信道也一起关掉了：无论用户名是否存在、处于什么状态，
     * 现在都会完整跑一次 BCrypt 比对，响应时间不再暴露「这个账号不存在」。
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(user -> {
            // 刻意留空：状态判定必须发生在口令校验之后，见上面的说明
        });
        provider.setPostAuthenticationChecks(user -> {
            if (!user.isEnabled()) {
                throw new DisabledException("账号尚未通过审核或已被禁用");
            }
        });
        return provider;
    }

    /** 显式装配全局 AuthenticationManager，确保登录走的就是上面这个 provider */
    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    /** 登录成功后由控制器手动把认证结果写回 Session */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           SecurityContextRepository securityContextRepository,
                                           UserRepository userRepository) throws Exception {
        // 会话新鲜度校验必须排在鉴权之前：降权/停用要立刻生效，不能等会话自然过期
        SessionFreshnessFilter sessionFreshnessFilter = new SessionFreshnessFilter(userRepository);
        // 前端是单页应用，CSRF 令牌从可读的 Cookie 中取，由 axios 自动回填到请求头
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfRequestHandler.setCsrfRequestAttributeName(null);

        http
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(reg -> reg
                // ---------- 公开接口 ----------
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                // 未登录访客可以浏览公开的物种与生态系统信息，但统计类接口不能跟着
                // 下面那条 GET 通配规则一起公开出去：/api/ecosystems/stats 带着每个
                // 生态系统的观测次数与发现物种数，属于报告 表 3-2 里对公众标 × 的
                // 「查看统计报表与分布地图」，学生才是 √。
                //
                // PUBLIC 同样是「已登录用户」，所以这三处只写 authenticated() 拦不住公众，
                // 必须点明放行哪三个角色。
                .requestMatchers(HttpMethod.GET, "/api/ecosystems/stats", "/api/observations/map",
                        "/api/stats/dashboard").hasAnyRole(STATS_ROLES)
                .requestMatchers(HttpMethod.GET, "/api/species/**", "/api/ecosystems/**").permitAll()
                // 图像识别对公众与学生开放（矩阵 √√√√），而识别的第一步就是上传照片，
                // 所以下面的 /api/upload 不能跟着物种/观测的写入规则一起只放行科研人员，
                // 否则学生与公众点「开始识别」会直接拿到 403。
                .requestMatchers(HttpMethod.POST, "/api/upload").authenticated()
                // ---------- 模块五：按能力细分，必须排在兜底的 /api/** 之前 ----------
                // 物种描述智能补全（矩阵 √√××）
                .requestMatchers(HttpMethod.POST, "/api/ai/complete").hasAnyRole("ADMIN", "RESEARCHER")
                // 观测智能标签与异常检测、智能问答（矩阵 √√√×）
                .requestMatchers(HttpMethod.POST, "/api/ai/analyze", "/api/ai/ask")
                        .hasAnyRole(STATS_ROLES)
                .requestMatchers("/api/ai/history").hasAnyRole(STATS_ROLES)
                // 其余 /api/ai/**（identify、translate、records）矩阵为 √√√√，落到下面的兜底规则
                // 任何登录用户都能改自己的资料和密码，必须排在下面的管理员规则之前
                .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/password").authenticated()
                // ---------- 管理员专属 ----------
                .requestMatchers("/api/users/**", "/api/logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/species/**", "/api/ecosystems/**").hasRole("ADMIN")
                // ---------- 科研人员与管理员 ----------
                .requestMatchers(HttpMethod.POST, "/api/species/**", "/api/ecosystems/**", "/api/observations/**")
                        .hasAnyRole("ADMIN", "RESEARCHER")
                .requestMatchers(HttpMethod.PUT, "/api/species/**", "/api/ecosystems/**", "/api/observations/**")
                        .hasAnyRole("ADMIN", "RESEARCHER")
                // 观测记录删除对科研人员开放，但是否属于自己的记录由业务层再判断
                .requestMatchers(HttpMethod.DELETE, "/api/observations/**").hasAnyRole("ADMIN", "RESEARCHER")
                // ---------- 其余接口一律需要登录 ----------
                .requestMatchers("/api/**").authenticated()
                // 前端静态资源与单页应用路由
                .anyRequest().permitAll())
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfRequestHandler))
            // 放在鉴权之前：降权、停用、改密码必须立刻生效
            .addFilterBefore(sessionFreshnessFilter, AuthorizationFilter.class)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req, res, auth) -> writeJson(res, 200, true, "已退出登录", null))
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            // 认证失败与权限不足由过滤器链直接短路，不会走到 @RestControllerAdvice，
            // 所以这里必须自己拼出与业务接口一致的 Result 结构，前端才能读到提示语。
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeJson(res, 401, false, "登录状态已失效，请重新登录", null))
                .accessDeniedHandler((req, res, e) -> {
                    // 判断「是不是已登录」必须用 SecurityContextHolder，不能用 req.getUserPrincipal()。
                    // 原先用的是后者，真机上跑出过与注释相反的结论：CsrfFilter 抛出的
                    // AccessDeniedException 由 ExceptionTranslationFilter 处理，此时容器级 principal
                    // 还没被填上（Spring Security 不设置 Tomcat 的 user principal），于是
                    // 一个已经登录、只是 CSRF 令牌过期的用户收到的是 401「登录状态已失效」——
                    // 前端 request.js 见到 401 就 clear() 本地状态并跳登录页，
                    // 结果用户填了很久的表单直接作废。MockMvc 里
                    // SecurityMockMvcRequestPostProcessors.authentication() 会顺手把
                    // request.setUserPrincipal 设好，所以这条在单元测试里永远看不出来。
                    var authentication = SecurityContextHolder.getContext().getAuthentication();
                    boolean loggedIn = authentication != null
                            && authentication.isAuthenticated()
                            && !(authentication instanceof AnonymousAuthenticationToken);
                    if (e instanceof CsrfException && loggedIn) {
                        // 令牌问题不是权限问题，更不该被踢下线：告诉他刷新页面重投一次
                        writeJson(res, 403, false,
                                "页面的安全令牌已过期（通常是停留太久），请刷新页面后重新提交", null);
                        return;
                    }
                    // 未登录必须回 401，不能回 403。README 第 8 节与报告 8.1 写的是
                    // 「401 = 会话失效或未登录，前端自动跳登录页；403 = 角色不够」。
                    // 匿名发起的 POST/PUT/DELETE 会先被 CsrfFilter 拦下并抛 AccessDeniedException，
                    // 于是原先回的是 403——前端 request.js 只对 401 做跳转，
                    // 结果会话刚过期就点保存的用户只会看到一句「权限不足」，
                    // 停在原地刷不出登录框，只能手工改地址栏。
                    if (loggedIn) {
                        writeJson(res, 403, false, "当前角色没有执行该操作的权限", null);
                    } else {
                        writeJson(res, 401, false, "登录状态已失效，请重新登录", null);
                    }
                }));

        return http.build();
    }

    private static void writeJson(HttpServletResponse res, int status, boolean success,
                                  String message, Object data) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"success\":" + success + ",\"message\":\"" + message + "\",\"data\":" + data + "}");
    }
}
