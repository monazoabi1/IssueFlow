package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.AuditLogResponse;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.service.AuditLogService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // -------------------------------------------------------------
    // get the audit logs
    //-----------------------------------------------------------
    @GetMapping
    public List<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) ActorType actor,
            @RequestParam(required = false) Long performedBy) {
        return auditLogService.getAuditLogs(entityType, entityId, action, actor, performedBy).stream()
                .map(AuditLogResponse::new)
                .collect(Collectors.toList());
    }
}
