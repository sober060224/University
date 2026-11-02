package com.gdou.marinebio.service;

import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.entity.OperationLog;
import com.gdou.marinebio.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 模块一：用户活动日志记录。
 * 物种删除、账号审核、大模型调用等关键操作都会留痕，便于追溯与质量分析。
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final OperationLogRepository logRepository;

    /**
     * 审计行必须自己提交，不能跟着业务事务一起回滚。
     *
     * <p>record() 的 12 个调用点都写在带 @Transactional 的服务方法里
     * （UserService 审核/改密/改角色、SpeciesService 与 ObservationService 的增删改），
     * 默认传播行为是加入调用方事务：业务在提交期唯一键冲突、或任何一步抛异常回滚时，
     * 这条审计记录也跟着没了。偏偏最该留痕的就是「审核通过」「重置密码」「调整角色」
     * 这三条——失败的那次操作在日志里查不到，等于日志只记录成功的、不记录出事的。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(LoginUser user, String module, String operation,
                       String targetType, Integer targetId, String detail) {
        OperationLog entry = new OperationLog();
        entry.setUserId(user == null ? null : user.getId());
        entry.setUsername(user == null ? "匿名" : cut50(user.getUsername()));
        entry.setModule(module);
        entry.setOperation(operation);
        entry.setTargetType(targetType);
        entry.setTargetId(targetId);
        // detail 列是 VARCHAR(500)：传入的明细里有物种名、文件名、用户名等用户输入，
        // 超过 500 字会被 MySQL 直接拒掉（Data too long），整条审计记录连带事务一起失败。
        // 这里按列宽截断，保证「操作发生过」这件事一定留痕。
        entry.setDetail(detail == null || detail.length() <= 500 ? detail : detail.substring(0, 499) + "…");
        entry.setIp(currentIp());
        try {
            // 必须 saveAndFlush：save() 只把实体放进持久化上下文，INSERT 要到事务提交时才发，
            // 而那时已在本方法的 catch 之外。于是「列宽/约束导致写日志失败」根本拦不住，
            // 异常会在提交期抛给调用方，把业务操作一起回滚——正是这个方法要防的事。
            logRepository.saveAndFlush(entry);
        } catch (RuntimeException e) {
            // 审计写失败不能把业务操作一起拖下水：这张日志表挂了、上传却已经落盘，
            // 结果是「图片传成功、接口回 500」，用户重传一次，磁盘上多一张重复文件。
            // 这里吞掉异常只记 error：审计缺失是运维问题，业务失败是用户问题，
            // 后者更不该由前者引发。
            log.error("写入操作日志失败：{}", e.getMessage(), e);
        }
    }

    /**
     * username 列是 VARCHAR(50)：超长会被 MySQL 以 Data too long 拒掉，
     * 把整条审计记录连带业务事务一起打掉，而 username 是可以从登录接口直接塞进来的用户输入。
     */
    private static String cut50(String value) {
        return value == null || value.length() <= 50 ? value : value.substring(0, 49) + "…";
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
