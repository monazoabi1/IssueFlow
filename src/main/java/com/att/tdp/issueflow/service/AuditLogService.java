package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.AuditLogEntity;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------
    // log the audit log:
    // save the audit log to the database
    //-----------------------------------------------------------
    @Transactional
    public void log(AuditAction action, EntityType entityType, Long entityId) {
        log(action, entityType, entityId, resolvePerformedBy(), ActorType.USER);
    }

    // -------------------------------------------------------------
    // log the audit log:
    // save the audit log to the database
    //-----------------------------------------------------------
    @Transactional
    public void log(
            AuditAction action,
            EntityType entityType,
            Long entityId,
            Long performedBy,
            ActorType actor) {
        auditLogRepository.save(new AuditLogEntity(action, entityType, entityId, performedBy, actor));
    }

    // -------------------------------------------------------------
    // get the audit logs
    //-----------------------------------------------------------
    @Transactional(readOnly = true)
    public List<AuditLogEntity> getAuditLogs(
            EntityType entityType,
            Long entityId,
            AuditAction action,
            ActorType actor,
            Long performedBy) {
        Specification<AuditLogEntity> spec = Specification.where(null);
        if (entityType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (actor != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actor"), actor));
        }
        if (performedBy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("performedBy"), performedBy));
        }
        return auditLogRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    // -------------------------------------------------------------
    // resolve the performed by
    // return the user id of the user who performed the action
    //-----------------------------------------------------------
    private Long resolvePerformedBy() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository
                .findByUsername(authentication.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}
