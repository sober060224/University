package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationLogRepository extends JpaRepository<OperationLog, Integer> {

    /**
     * 日志检索。
     *
     * <p>shortcut: 三个条件都是「:x is null or ...」写法，MySQL 用不上索引，
     * `%username%` 还会强制全表扫；但这是 ADMIN 专属接口、分页上限 200、
     * 审计日志当前千行量级，一次全扫在毫秒级。当日志过万行或检索变成高频操作时，
     * 再换成动态 SQL + (create_time) 倒序索引。
     */
    @Query("""
            select l from OperationLog l where
              (:module is null or l.module = :module)
              and (:username is null or lower(l.username) like lower(concat('%', replace(replace(replace(:username, '!', '!!'), '%', '!%'), '_', '!_'), '%')) escape '!')
              and (:operation is null or l.operation = :operation)
            """)
    Page<OperationLog> search(String module, String username, String operation, Pageable pageable);
}
