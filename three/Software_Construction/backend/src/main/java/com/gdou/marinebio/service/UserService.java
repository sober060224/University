package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.common.PageResult;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块一：用户注册与审核、用户信息维护、用户角色管理。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    /** 注册申请：学生与公众可自助申请，提交后状态为待审核 */
    @Transactional
    public User register(AuthForms.Register form) {
        if (userRepository.existsByUsername(form.username())) {
            throw new BizException("用户名「" + form.username() + "」已被占用");
        }
        Role applyRole = parseRole(form.applyRole(), Role.PUBLIC);
        if (applyRole != Role.STUDENT && applyRole != Role.PUBLIC) {
            throw new BizException("只能申请学生或公众身份，科研人员与管理员由管理员分配");
        }
        User user = new User();
        user.setUsername(form.username());
        user.setPassword(passwordEncoder.encode(form.password()));
        user.setRealName(form.realName());
        user.setRole(applyRole);
        user.setStatus(UserStatus.PENDING);
        user.setPhone(form.phone());
        user.setEmail(form.email());
        user.setStudentNo(form.studentNo());
        User saved = userRepository.save(user);

        logService.record(new LoginUser(saved), "模块一", "用户注册", "用户", saved.getId(),
                "提交注册申请，身份：" + applyRole.name() + "，状态：待审核");
        return saved;
    }

    /** 管理员审核：激活并可同时授予科研人员身份，或驳回 */
    @Transactional
    public User approve(Integer id, AuthForms.Approve form, LoginUser operator) {
        User user = require(id);
        boolean approved = form.approved() == null || form.approved();
        user.setStatus(approved ? UserStatus.ACTIVE : UserStatus.REJECTED);
        if (approved && form.role() != null && !form.role().isBlank()) {
            user.setRole(parseRole(form.role(), user.getRole()));
        }
        User saved = userRepository.save(user);
        logService.record(operator, "模块一", approved ? "审核通过" : "审核驳回", "用户", id,
                "用户「" + user.getUsername() + "」" + (approved ? "已激活" : "被驳回"));
        return saved;
    }

    public PageResult<User> search(UserStatus status, Role role, String keyword, Pageable pageable) {
        return PageResult.of(userRepository.search(status, role, keyword, pageable));
    }

    public User get(Integer id) {
        return require(id);
    }

    /** 用户维护自己的个人信息 */
    @Transactional
    public User updateProfile(Integer id, AuthForms.Profile form) {
        User user = require(id);
        if (form.realName() != null && !form.realName().isBlank()) {
            user.setRealName(form.realName());
        }
        if (form.gender() != null) user.setGender(form.gender());
        if (form.phone() != null) user.setPhone(form.phone());
        if (form.email() != null) user.setEmail(form.email());
        if (form.avatar() != null) user.setAvatar(form.avatar());
        if (form.studentNo() != null) user.setStudentNo(form.studentNo());
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(LoginUser loginUser, String oldPassword, String newPassword) {
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BizException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** 管理员重置他人密码 */
    @Transactional
    public void resetPassword(Integer id, String newPassword, LoginUser operator) {
        User user = require(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logService.record(operator, "模块一", "重置密码", "用户", id, "重置用户「" + user.getUsername() + "」的登录密码");
    }

    @Transactional
    public User assignRole(Integer id, String role, LoginUser operator) {
        User user = require(id);
        Role newRole = parseRole(role, user.getRole());
        // 只在「把最后一名管理员改成别的角色」时拦截；授予 ADMIN 不受限制，
        // 否则系统里只剩一名管理员时反而无法再增设第二名。
        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new BizException("系统至少需要保留一名管理员");
        }
        user.setRole(newRole);
        User saved = userRepository.save(user);
        logService.record(operator, "模块一", "调整角色", "用户", id,
                "用户「" + user.getUsername() + "」角色调整为 " + newRole.name());
        return saved;
    }

    /** 供综合数据看板使用的用户概览 */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", userRepository.count());
        data.put("active", userRepository.countByStatus(UserStatus.ACTIVE));
        data.put("pending", userRepository.countByStatus(UserStatus.PENDING));
        data.put("researcher", userRepository.countByRole(Role.RESEARCHER));
        return data;
    }

    private User require(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new BizException("用户不存在"));
    }

    private Role parseRole(String raw, Role fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException("未知的角色：" + raw);
        }
    }
}
