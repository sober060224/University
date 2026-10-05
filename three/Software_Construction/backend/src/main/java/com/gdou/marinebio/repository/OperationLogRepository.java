package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OperationLogRepository extends JpaRepository<OperationLog, Integer> {

    @Query("""
            select l from OperationLog l where
              (:module is null or l.module = :module)
              and (:username is null or lower(l.username) like lower(concat('%', :username, '%')))
              and (:operation is null or l.operation = :operation)
            """)
    Page<OperationLog> search(String module, String username, String operation, Pageable pageable);
}
