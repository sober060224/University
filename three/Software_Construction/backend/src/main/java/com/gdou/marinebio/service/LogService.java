package com.gdou.marinebio.service;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.OperationLog;
import com.gdou.marinebio.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 模块一：用户活动日志记录。
 * 物种删除、账号审核、大模型调用等关键操作都会留痕，便于追溯与质量分析。
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final OperationLogRepository logRepository;

    public void record(LoginUser user, String module, String operation,
                       String targetType, Integer targetId, String detail) {
        OperationLog log = new OperationLog();
        log.setUserId(user == null ? null : user.getId());
        log.setUsername(user == null ? "匿名" : user.getUsername());
        log.setModule(module);
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIp(currentIp());
        logRepository.save(log);
    }

    /**
     * 只取直连地址，不采信客户端自带的 X-Forwarded-For：
     * 该头任何人随手就能伪造，写进审计日志等于日志可以被伪造。
     * 将来若真要挂在 Nginx 之后，改用 server.forward-headers-strategy=framework 统一处理。
     */
    private String currentIp() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
