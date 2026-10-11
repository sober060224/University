package com.gdou.marinebio.service;

import com.gdou.marinebio.common.BizException;
import com.gdou.marinebio.common.LoginUser;
import com.gdou.marinebio.dto.AuthForms;
import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import com.gdou.marinebio.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 「系统至少保留一名在岗管理员」这条保命规则的单测。
 *
 * <p>为什么值得单独测：这条规则失手的后果不是报错，而是**整个后台永久锁死**——
 * 一旦不再存在 status=ACTIVE 的 ADMIN，/api/users/** 与 /api/logs/** 只放行 ADMIN，
 * 谁也登不进去，只能手工改库救回来。而它原来只在 assignRole 里写了一次，
 * approve 同样能改角色、还能改状态，等于留了一条绕过它的正道。
 *
 * <p>用 Mockito 而不起 Spring 上下文：这里要验的是「判断条件本身」，
 * 悲观锁由 {@code lockAllByRole} 的注解负责，那是数据库层的事，本机没有 MySQL 也能测。
 */
@ExtendWith(MockitoExtension.class)
class AdminGuardTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LogService logService;

    @InjectMocks
    private UserService userService;

    private static final LoginUser ADMIN_OPERATOR = new LoginUser(account(9, "root", Role.ADMIN, UserStatus.ACTIVE));

    private static User account(Integer id, String username, Role role, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(username);
        user.setPassword("hash");
        user.setRole(role);
        user.setStatus(status);
        user.setCredentialVersion(1);
        return user;
    }

    private void givenTarget(User target, User... otherAdmins) {
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        // lenient：审核结果如果让目标本人继续保有 ACTIVE 的 ADMIN 身份，
        // 判据会直接短路返回，根本不会去查管理员集合——这正是要验证的行为之一。
        lenient().when(userRepository.lockAllByRole(Role.ADMIN))
                .thenReturn(java.util.stream.Stream
                        .concat(java.util.stream.Stream.of(target), java.util.Arrays.stream(otherAdmins))
                        .toList());
        lenient().when(userRepository.save(any(User.class))).thenAnswer(c -> c.getArgument(0));
    }

    @Test
    @DisplayName("审核时把唯一在岗管理员改成学生 —— 必须拦住")
    void approveCannotDemoteTheOnlyActiveAdmin() {
        User only = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(only);

        assertThatThrownBy(() -> userService.approve(1, new AuthForms.Approve(true, "STUDENT"), ADMIN_OPERATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("在岗管理员");
    }

    @Test
    @DisplayName("对唯一在岗管理员点「驳回」同样会锁死后台 —— 必须拦住")
    void approveCannotRejectTheOnlyActiveAdmin() {
        // 这条是本次修复的核心：驳回不改角色，所以「只看角色变化」的旧判据完全放行，
        // 而 status 变成 REJECTED 之后 SessionFreshnessFilter 会立刻作废他的会话，效果与降级相同。
        User only = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(only);

        assertThatThrownBy(() -> userService.approve(1, new AuthForms.Approve(false, null), ADMIN_OPERATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("在岗管理员");
    }

    @Test
    @DisplayName("另一名管理员虽然存在但仍在待审核 —— 降级当前这位照样锁死，必须拦住")
    void pendingAdminDoesNotCountAsSafetyNet() {
        User activeAdmin = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        User pendingAdmin = account(5, "pending", Role.ADMIN, UserStatus.PENDING);
        givenTarget(activeAdmin, pendingAdmin);

        // 旧判据是 lockAllByRole(ADMIN).size() <= 1，这里 size=2 会放行，
        // 于是把唯一「在岗」的降级后只剩一个登得进去的账号都没有的管理员。
        assertThatThrownBy(() -> userService.assignRole(1, "STUDENT", ADMIN_OPERATOR))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("在岗管理员");
    }

    @Test
    @DisplayName("还有第二名在岗管理员时，降级与驳回都照常放行")
    void demoteIsAllowedWhileAnotherActiveAdminRemains() {
        User target = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        User peer = account(2, "admin2", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(target, peer);

        assertThatCode(() -> userService.assignRole(1, "RESEARCHER", ADMIN_OPERATOR)).doesNotThrowAnyException();
        assertThat(target.getRole()).isEqualTo(Role.RESEARCHER);

        User target2 = account(3, "admin3", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(target2, peer);
        assertThatCode(() -> userService.approve(3, new AuthForms.Approve(false, null), ADMIN_OPERATOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("授予 ADMIN 不受限制，否则只剩一名管理员时无法增设第二名")
    void promotingToAdminIsAlwaysAllowed() {
        User researcher = account(4, "researcher", Role.RESEARCHER, UserStatus.ACTIVE);
        when(userRepository.findById(4)).thenReturn(Optional.of(researcher));
        when(userRepository.save(any(User.class))).thenAnswer(c -> c.getArgument(0));

        assertThatCode(() -> userService.assignRole(4, "ADMIN", ADMIN_OPERATOR)).doesNotThrowAnyException();
        assertThat(researcher.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("审核通过并保持管理员身份不会被自己的规则误伤")
    void approvingAdminStaysAdminIsFine() {
        User only = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(only);

        assertThatCode(() -> userService.approve(1, new AuthForms.Approve(true, "ADMIN"), ADMIN_OPERATOR))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("缺 approved 字段按「不通过」处理，残缺请求体不能激活账号")
    void missingApprovedFlagFailsClosed() {
        User applicant = account(7, "pending", Role.STUDENT, UserStatus.PENDING);
        when(userRepository.findById(7)).thenReturn(Optional.of(applicant));
        when(userRepository.save(any(User.class))).thenAnswer(c -> c.getArgument(0));

        User result = userService.approve(7, new AuthForms.Approve(null, null), ADMIN_OPERATOR);

        assertThat(result.getStatus()).isEqualTo(UserStatus.REJECTED);
    }

    @Test
    @DisplayName("审核与改角色都会自增 credentialVersion，旧会话必须立刻作废")
    void credentialVersionBumpsOnEveryPrivilegeChange() {
        User target = account(1, "admin", Role.ADMIN, UserStatus.ACTIVE);
        User peer = account(2, "admin2", Role.ADMIN, UserStatus.ACTIVE);
        givenTarget(target, peer);

        userService.assignRole(1, "STUDENT", ADMIN_OPERATOR);

        assertThat(target.getCredentialVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("注册只允许申请学生与公众，不能自助要一个科研人员身份")
    void registerRejectsSelfGrantedPrivilegedRoles() {
        when(userRepository.existsByUsername("soul")).thenReturn(false);

        assertThatThrownBy(() -> userService.register(
                new AuthForms.Register("soul", "secret123", "某人", "RESEARCHER", null, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("只能申请学生或公众");

        assertThatThrownBy(() -> userService.register(
                new AuthForms.Register("soul", "secret123", "某人", "ADMIN", null, null, null)))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("注册申请落库时状态一定是待审核，且密码是加密后的而不是明文")
    void registerStoresPendingUserWithEncodedPassword() {
        when(userRepository.existsByUsername("soul")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(c -> c.getArgument(0));

        User saved = userService.register(
                new AuthForms.Register("soul", "secret123", "某人", "STUDENT", null, null, null));

        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getPassword()).isEqualTo("$2a$10$encoded");
        assertThat(saved.getPassword()).isNotEqualTo("secret123");
    }

    @Test
    @DisplayName("审核通过时会把状态、角色一并写入，并给出一条可读的审计记录")
    void approveWritesAuditLog() {
        User applicant = account(8, "newbie", Role.STUDENT, UserStatus.PENDING);
        when(userRepository.findById(8)).thenReturn(Optional.of(applicant));
        when(userRepository.save(any(User.class))).thenAnswer(c -> c.getArgument(0));

        userService.approve(8, new AuthForms.Approve(true, "RESEARCHER"), ADMIN_OPERATOR);

        assertThat(applicant.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(applicant.getRole()).isEqualTo(Role.RESEARCHER);
        org.mockito.Mockito.verify(logService).record(any(), any(), org.mockito.ArgumentMatchers.eq("审核通过"),
                any(), anyInt(), any());
    }
}
