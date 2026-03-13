package com.example.agentic_ai.service;

import com.example.agentic_ai.domain.AuditRecord;
import com.example.agentic_ai.repository.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    @Value("${app.audit.enabled:true}")
    private boolean auditEnabled;

    @Async
    public void log(String query, String response, String model, String requestType,
                    Long responseTimeMs, Integer contextDocs, String retrievedContext) {
        if (!auditEnabled) return;
        try {
            AuditRecord record = new AuditRecord(query, response, model, requestType,
                    responseTimeMs, contextDocs, retrievedContext);
            auditRepository.save(record);
            log.info("[AUDIT] type={} model={} responseTimeMs={} contextDocs={}",
                    requestType, model, responseTimeMs, contextDocs);
        } catch (Exception e) {
            log.error("[AUDIT] Failed to save audit record: {}", e.getMessage());
        }
    }

    public List<AuditRecord> getRecentAuditLogs() {
        return auditRepository.findTop50ByOrderByTimestampDesc();
    }

    public List<AuditRecord> getAuditLogsByType(String requestType) {
        return auditRepository.findByRequestType(requestType);
    }
}
