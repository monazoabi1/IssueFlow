package com.att.tdp.issueflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class TicketEntity {

    @NotBlank
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank
    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    private TicketType type;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long ticketId;

    private long projectId;

    // Long - can be null if the ticket is not assigned to any user
    private Long assigneeId;

    private Instant dueDate;

    // ticket version for optimistic locking - no conflicts on the version
    @Version
    private long version;

    private boolean isDeleted;

    @Column(name = "is_overdue")
    private boolean overdue;

    public enum TicketStatus {
        TODO,
        IN_PROGRESS,
        IN_REVIEW,
        DONE
    }

    public enum TicketPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum TicketType {
        BUG,
        FEATURE,
        TECHNICAL
    }

    protected TicketEntity() {}

    public TicketEntity(String title, String description, TicketStatus status, TicketPriority priority,
            TicketType type, long projectId) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.type = type;
        this.projectId = projectId;
        this.isDeleted = false;
        this.overdue = false;
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

    public long getTicketId() {
        return ticketId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public long getVersion() {
        return version;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isOverdue() {
        return overdue;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public void setType(TicketType type) {
        this.type = type;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}
