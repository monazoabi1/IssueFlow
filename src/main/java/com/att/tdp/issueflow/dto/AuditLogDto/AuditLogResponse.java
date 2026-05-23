package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.AuditLogEntity;
import java.time.Instant;

public class AuditLogResponse {

    private final long id;
    private final String action;
    private final String entityType;
    private final long entityId;
    private final Long performedBy;
    private final String actor;
    private final Instant timestamp;

    public AuditLogResponse(AuditLogEntity log) {
        this.id = log.getId();
        this.action = log.getAction().name();
        this.entityType = log.getEntityType().name();
        this.entityId = log.getEntityId();
        this.performedBy = log.getPerformedBy();
        this.actor = log.getActor().name();
        this.timestamp = log.getTimestamp();
    }

    public long getId() {
        return id;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public long getEntityId() {
        return entityId;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public String getActor() {
        return actor;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
