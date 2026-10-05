package com.gdou.marinebio.repository;

import com.gdou.marinebio.entity.AiRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecordRepository extends JpaRepository<AiRecord, Integer> {

    Page<AiRecord> findByUserId(Integer userId, Pageable pageable);
}
