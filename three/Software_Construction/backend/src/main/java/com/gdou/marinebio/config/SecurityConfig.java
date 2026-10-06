package com.gdou.marinebio.config;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
            // 未通过审核的账号不允许登录
            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new DisabledException("账号尚未通过审核");
            }
            return new LoginUser(user);
        };
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService,
                                                           PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
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
                // 未登录访客可以浏览公开的物种与生态系统信息。
                // 但 /api/ecosystems/stats 带有各生态系统的观测次数与物种数，
                // 且统计没有按 is_public 过滤，会把未公开物种的数量一起暴露出去，
                // 所以它不能跟着下面的通配规则一起公开。
                .requestMatchers(HttpMethod.GET, "/api/ecosystems/stats").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/species/**", "/api/ecosystems/**").permitAll()
                // 任何登录用户都能改自己的资料和密码，必须排在下面的管理员规则之前
                .requestMatchers(HttpMethod.PUT, "/api/users/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/users/password").authenticated()
                // ---------- 管理员专属 ----------
                .requestMatchers("/api/users/**", "/api/logs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/species/**", "/api/ecosystems/**").hasRole("ADMIN")
                // ---------- 科研人员与管理员 ----------
                .requestMatchers(HttpMethod.POST, "/api/species/**", "/api/ecosystems/**", "/api/observations/**",
                        "/api/upload").hasAnyRole("ADMIN", "RESEARCHER")
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
                    boolean loggedIn = req.getUserPrincipal() != null;
                    writeJson(res, 403, false,
                            loggedIn ? "当前角色没有执行该操作的权限" : "请先登录后再操作", null);
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
