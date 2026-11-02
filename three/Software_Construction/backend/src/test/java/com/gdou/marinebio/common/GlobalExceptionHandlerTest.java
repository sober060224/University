package com.gdou.marinebio.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.gdou.marinebio.entity.Role;
import jakarta.validation.Valid;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器的状态码。
 *
 * <p>为什么单独写这一个测试：Result.fail 默认落在 HTTP 200 上，而这个文件里的处理器
 * 靠的是注解或 ResponseEntity 往外撑状态码 —— 少一个 @ResponseStatus 或写回
 * Result 而不是 ResponseEntity，响应就从 500 悄悄退回 200。编译期看不出来，
 * 只断言响应体的测试也看不出来（success 都是 false、message 都是同一句中文），
 * 唯独断言 HTTP 状态能抓住。这类「静默失败」正是监控完全看不出异常的那一种。
 *
 * <p>用 standaloneSetup 只装载一个会抛异常的测试控制器 + 真实的 GlobalExceptionHandler：
 * 不需要容器、不需要数据库，测的就是「异常 → HTTP 状态」这一段映射本身。
 */
class GlobalExceptionHandlerTest {

    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new BoomController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("兜底分支必须回 500，不能落成 200")
    void fallbackIsServerError() throws Exception {
        mvc.perform(get("/boom/npe").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("服务器内部错误，请稍后重试"));
    }

    @Test
    @DisplayName("业务异常是客户端错误，回 400")
    void bizExceptionIsBadRequest() throws Exception {
        mvc.perform(get("/boom/biz").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("认证/校验/禁用类异常同样不能落在 200：只看状态码的调用方才会把它们统计成成功")
    void authAndValidationHandlersCarryTheirOwnStatus() throws Exception {
        mvc.perform(post("/boom/valid").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/boom/bad-credentials").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/boom/auth").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/boom/disabled").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("枚举参数写错时，错误文案要列出合法值")
    void enumTypeMismatchListsAllowedValues() throws Exception {
        mvc.perform(get("/boom/enum").param("role", "WIZARD").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("ADMIN")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RESEARCHER")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PUBLIC")));
    }

    @RestController
    static class BoomController {
        @GetMapping("/boom/npe")
        String npe() {
            throw new NullPointerException("故意制造一次未预期异常");
        }

        @GetMapping("/boom/biz")
        String biz() {
            throw new BizException("物种不存在");
        }

        @PostMapping("/boom/valid")
        String valid(@Valid @RequestBody SpeciesPayload payload) {
            return payload.name();
        }

        @GetMapping("/boom/bad-credentials")
        String badCredentials() {
            throw new BadCredentialsException("bad credentials");
        }

        @GetMapping("/boom/auth")
        String auth() {
            throw new InternalAuthenticationServiceException("auth");
        }

        @GetMapping("/boom/disabled")
        String disabled() {
            throw new DisabledException("disabled");
        }

        @GetMapping("/boom/enum")
        String enumParam(@org.springframework.web.bind.annotation.RequestParam Role role) {
            return role.name();
        }
    }

    /** 只用来触发一次 @Valid 失败：name 留空就会进 MethodArgumentNotValidException。 */
    record SpeciesPayload(@jakarta.validation.constraints.NotBlank(message = "名字不能为空") String name) {
    }
}
