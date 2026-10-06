package com.gdou.marinebio.common;

import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：统一转换成 { success:false, message:'中文提示' }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常，例如「物种不存在」「用户名已被占用」 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.fail(e.getMessage());
    }

    /**
     * @Valid 校验失败。
     * 一次性返回所有字段错误，而不是只返第一条：否则一张表里有多个字段不合规时，
     * 用户得来回提交好几轮才能改完。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(m -> m != null && !m.isBlank())
                .distinct()
                .reduce((a, b) -> a + "；" + b)
                .orElse("参数校验失败");
        return Result.fail(message);
    }

    /** 登录失败：用户名或密码错误 */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentials(BadCredentialsException e) {
        return Result.fail("用户名或密码错误");
    }

    /** 账号未通过审核或被禁用 */
    @ExceptionHandler(DisabledException.class)
    public Result<Void> handleDisabled(DisabledException e) {
        return Result.fail("账号未通过审核或已被禁用，请联系管理员");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuth(AuthenticationException e) {
        return Result.fail("认证失败，请重新登录");
    }

    /** 越权访问 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.fail("没有权限执行该操作");
    }

    /**
     * 请求体不是合法 JSON（少了逗号、类型写错等）。属于客户端错误，
     * 不该落进兜底分支让前端显示「服务器内部错误」。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail("请求数据格式有误，请检查后重试");
    }

    /** 查询串类型不匹配，例如 /api/users?id=abc、/api/observations?page=x */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail("参数「" + e.getName() + "」的值不合法");
    }

    /** 缺少必填查询参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail("缺少必填参数「" + e.getParameterName() + "」");
    }

    /** 超出 spring.servlet.multipart.max-file-size 限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        // 触发本异常的原因有两个：单文件超过 max-file-size，或整个请求超过 max-request-size。
        // 只写 5MB 会让「多文件请求总量超限」这种情况显示一个错误的阈值。
        return Result.fail("文件过大，请压缩后重试（单文件不超过 5MB，单次请求不超过 10MB）");
    }

    /**
     * 数据完整性冲突，典型来源是唯一键。
     *
     * <p>范围比看上去大：Spring 会把 JDBC 层的异常包装成
     * DataIntegrityViolationException，同时把 DataAccessException 的其它子类
     * （比如查询超时、连接池耗尽）一并归到 @SpringDataWebSupport 转换出来的这一支，
     * 所以这里必须先分出「数据库不通」和「数据冲突」两种情况：
     * 前者是运维问题要留完整堆栈，后者是用户填错要给可读提示。
     *
     * <p>例如同一个物种在一次观测里被重复关联、或用户名被并发占用。
     * 服务层已经先查过一遍给出中文提示，这里处理的是两个请求同时通过检查的竞态，
     * 所以回一个 409 加可读说明，比落进兜底分支显示「服务器内部错误」有用得多。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleConflict(DataIntegrityViolationException e) {
        // 真正的唯一键/外键冲突才是「用户填重了」，其余都是基础设施问题
        if (isConstraintConflict(e)) {
            log.warn("数据完整性冲突", e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Result.fail("数据冲突：名称或关联关系已存在，请检查后重试"));
        }
        // 连接池耗尽、查询超时等：留完整堆栈，提示语说清是服务端问题
        log.error("数据访问异常", e);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.fail("服务暂时不可用，请稍后重试"));
    }

    /** 沿因果链找出根因，判断是不是唯一键/外键之类的约束冲突 */
    private static boolean isConstraintConflict(Throwable e) {
        Throwable root = e;
        while (root != null) {
            String msg = root.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                if (m.contains("duplicate entry") || m.contains("constraint")
                        || m.contains("integrity constraint") || m.contains("foreign key")) {
                    return true;
                }
            }
            if (root instanceof java.sql.SQLIntegrityConstraintViolationException
                    || root instanceof org.hibernate.exception.ConstraintViolationException) {
                return true;
            }
            root = root.getCause();
        }
        return false;
    }

    /**
     * 已经带好状态码的异常：接口路径写错（NoResourceFoundException，404）、
     * 请求方法不支持（HttpRequestMethodNotSupportedException，405）等。
     *
     * <p>这些异常在 Spring 6 里都继承 ServletException 并实现 ErrorResponse 接口，
     * 并没有共同的 Exception 基类，所以这里按 ServletException 捕获、再用
     * instanceof ErrorResponse 取它们各自的状态码。
     *
     * <p>必须保住状态码：ExceptionHandlerExceptionResolver 排在
     * DefaultHandlerExceptionResolver 之前，一旦落进下面的兜底分支就会变成
     * HTTP 200 +「服务器内部错误」，客户端只会以为该重试，监控也完全看不出异常。
     */
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<Result<Void>> handleServletError(ServletException e) {
        if (e instanceof ErrorResponse er) {
            if (er.getStatusCode().is5xxServerError()) {
                log.error("服务端异常: {}", e.getMessage(), e);
                return ResponseEntity.status(er.getStatusCode()).body(Result.fail("服务器内部错误，请稍后重试"));
            }
            log.warn("{} {} -> {}", er.getStatusCode(), e.getClass().getSimpleName(), e.getMessage());
            return ResponseEntity.status(er.getStatusCode()).body(Result.fail(describe(er.getStatusCode().value())));
        }
        // 不带状态码的 ServletException 没有更合适的说法，按内部错误处理
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(handleOther(e));
    }

    private static String describe(int status) {
        return switch (status) {
            case 404 -> "请求的接口不存在，请检查路径是否正确";
            case 405 -> "该接口不支持此请求方法";
            case 415 -> "请求的数据格式不受支持";
            default -> "请求无法处理";
        };
    }

    /** 兜底 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理的异常", e);
        return Result.fail("服务器内部错误，请稍后重试");
    }
}
