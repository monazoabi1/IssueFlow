package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class UpdateTicketRequest {

    /** Required for optimistic locking — must match the ticket version the client read. */
    @NotNull
    private Long version;

    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long assigneeId;
    private Instant dueDate;

    public void applyTo(TicketEntity ticket) {
        if (title != null) {
            ticket.setTitle(title);
        }
        if (description != null) {
            ticket.setDescription(description);
        }
        if (priority != null) {
            ticket.setPriority(priority);
        }
        if (assigneeId != null) {
            ticket.setAssigneeId(assigneeId);
        }
        if (dueDate != null) {
            ticket.setDueDate(dueDate);
        }
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }
}
