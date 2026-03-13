package com.example.agentic_ai.repository;

import com.example.agentic_ai.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByRequestType(String requestType);

    List<AuditRecord> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditRecord> findTop50ByOrderByTimestampDesc();
}
