package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class TicketResponse {

    private final long id;
    private final String title;
    private final String description;
    private final TicketStatus status;
    private final TicketPriority priority;
    private final TicketType type;
    private final long projectId;
    private final Long assigneeId;
    private final Instant dueDate;
    private final boolean isOverdue;
    private final long version;

    public TicketResponse(TicketEntity ticket) {
        this.id = ticket.getTicketId();
        this.title = ticket.getTitle();
        this.description = ticket.getDescription();
        this.status = ticket.getStatus();
        this.priority = ticket.getPriority();
        this.type = ticket.getType();
        this.projectId = ticket.getProjectId();
        this.assigneeId = ticket.getAssigneeId();
        this.dueDate = ticket.getDueDate();
        this.version = ticket.getVersion();
        this.isOverdue = ticket.isOverdue();
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketType getType() {
        return type;
    }

    public long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    @JsonProperty("isOverdue")
    public boolean isOverdue() {
        return isOverdue;
    }

    public long getVersion() {
        return version;
    }
}
