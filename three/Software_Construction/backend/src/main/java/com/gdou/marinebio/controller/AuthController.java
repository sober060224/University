package com.gdou.marinebio.controller;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.Result;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.repository.UserRepository;
import com.gdou.marinebio.service.LogService;
import com.gdou.marinebio.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块一：登录、注册、当前用户信息。
 *
 * 登录走 Spring Security 的 AuthenticationManager，认证成功后手工把 SecurityContext
 * 存回 session，这样后续请求由过滤器链自动识别登录态。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final LogService logService;

    /**
     * 前端首屏先调一次这个接口拿 XSRF-TOKEN cookie，
     * 之后 axios 会自动带上 X-XSRF-TOKEN 头，Spring Security 的 CSRF 校验就过了。
     */
    @GetMapping("/csrf")
    public Result<Map<String, Object>> csrf() {
        return Result.ok(Map.of("ready", true));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody AuthForms.Register form) {
        return Result.ok("注册申请已提交，等待管理员审核", profileOf(userService.register(form)));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AuthForms.Login form,
                                             HttpServletRequest request,
                                             HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.username(), form.password()));
        } catch (AuthenticationException ex) {
            // 只把「凭据类」异常压成同一句话：口令错、用户不存在都不能区分，否则
            // 就成了账号枚举接口。但「账号禁用/未审核」不是凭据问题 —— 把它也压成
            // 「用户名或密码错误」，用户会一直怀疑自己密码错了，永远找不到真正原因，
            // 而 GlobalExceptionHandler 已经为它准备了 403 + 准确的提示语。
            // 这条是 R3 的安全加固过头造成的回归，R4 退回。
            if (ex instanceof DisabledException) {
                throw ex;
            }
            throw new BizException("用户名或密码错误");
        }

        // 先建出会话再换 session id，最后把授权信息写进这个新会话
        // （formLogin() 已关闭，Spring 的 SessionFixationProtectionFilter 不会替我们做这件事）。
        // 顺序不能反：changeSessionId() 要求会话已存在，先 saveContext 再换 id 虽然能用，
        // 但换 id 与写授权分两步，中间任何一个请求插进来都可能读到“新 id + 空授权”。
        request.getSession(true);
        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        logService.record(loginUser, "模块一", "用户登录", "用户", loginUser.getId(), "登录成功");
        return Result.ok("登录成功", profileOf(userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BizException("用户不存在"))));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@AuthenticationPrincipal LoginUser loginUser) {
        if (loginUser == null) {
            return Result.ok(null);
        }
        return Result.ok(profileOf(userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BizException("用户不存在"))));
    }

    /** 只回前端要用的字段，密码绝不外泄。 */
    private Map<String, Object> profileOf(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("gender", user.getGender());
        map.put("phone", user.getPhone());
        map.put("email", user.getEmail());
        map.put("avatar", user.getAvatar());
        map.put("studentNo", user.getStudentNo());
        map.put("role", user.getRole() == null ? null : user.getRole().name());
        map.put("status", user.getStatus() == null ? null : user.getStatus().name());
        map.put("createTime", user.getCreateTime());
        return map;
    }
}
