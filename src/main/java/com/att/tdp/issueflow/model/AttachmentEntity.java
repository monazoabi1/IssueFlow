package com.att.tdp.issueflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attachments")
public class AttachmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private long ticketId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false)
    private byte[] data;

    protected AttachmentEntity() {}

    public AttachmentEntity(long ticketId, String filename, String contentType, long size, byte[] data) {
        this.ticketId = ticketId;
        this.filename = filename;
        this.contentType = contentType;
        this.size = size;
        this.data = data;
    }

    public Long getId() {
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

    public long getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }
}
