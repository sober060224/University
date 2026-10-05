package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.Role;
import com.gdou.marinebio.entity.User;
import com.gdou.marinebio.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByStatus(UserStatus status);

    long countByRole(Role role);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /** 管理端用户列表，支持按状态、角色、用户名/姓名关键词组合筛选 */
    @Query("""
            select u from User u where
              (:status is null or u.status = :status)
              and (:role is null or u.role = :role)
              and (:keyword is null or lower(u.username) like lower(concat('%', :keyword, '%'))
                   or lower(u.realName) like lower(concat('%', :keyword, '%')))
            """)
    Page<User> search(UserStatus status, Role role, String keyword, Pageable pageable);
}
