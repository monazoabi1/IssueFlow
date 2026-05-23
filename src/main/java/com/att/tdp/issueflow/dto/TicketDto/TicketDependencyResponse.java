package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;

public class TicketDependencyResponse {

    private final long id;
    private final String title;
    private final TicketStatus status;

    public TicketDependencyResponse(TicketEntity ticket) {
        this.id = ticket.getTicketId();
        this.title = ticket.getTitle();
        this.status = ticket.getStatus();
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public TicketStatus getStatus() {
        return status;
    }
}
