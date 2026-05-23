package com.att.tdp.issueflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        AUTO_ASSIGN
    }

    public enum EntityType {
        USER,
        PROJECT,
        TICKET,
        COMMENT
    }

    public enum ActorType {
        USER,
        SYSTEM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // id for the audit log
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "performed_by")
    private Long performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActorType actor;

    @Column(nullable = false)
    private Instant timestamp;

    protected AuditLogEntity() {}

    public AuditLogEntity(
            AuditAction action,
            EntityType entityType,
            Long entityId,
            Long performedBy,
            ActorType actor) {
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.performedBy = performedBy;
        this.actor = actor;
        this.timestamp = Instant.now();
    }

    @PreUpdate
    @PreRemove
    void preventMutation() {
        throw new UnsupportedOperationException("Audit logs are append-only");
    }

    public Long getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public ActorType getActor() {
        return actor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
