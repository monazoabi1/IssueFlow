package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.AttachmentEntity;

public class AttachmentResponse {

    private final long id;
    private final long ticketId;
    private final String filename;
    private final String contentType;

    public AttachmentResponse(AttachmentEntity attachment) {
        this.id = attachment.getId();
        this.ticketId = attachment.getTicketId();
        this.filename = attachment.getFilename();
        this.contentType = attachment.getContentType();
    }

    public long getId() {
        return id;
    }

    public long getTicketId() {
        return ticketId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }
}
