package com.att.tdp.issueflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "comments")
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long commentId;

    @NotBlank
    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketEntity ticket;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;


    protected CommentEntity() {}

    public CommentEntity(String content, TicketEntity ticket, UserEntity author) {
        this.content = content;
        this.ticket = ticket;
        this.author = author;
    }

    // -------------------------- GETTERS --------------------------
    public long getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }
    public TicketEntity getTicket() {
        return ticket;
    }
    public UserEntity getAuthor() {
        return author;
    }

    // -------------------------- SETTERS --------------------------
    public void setContent(String content) {
        this.content = content;
    }
    public void setTicket(TicketEntity ticket) {
        this.ticket = ticket;
    }
    public void setAuthor(UserEntity author) {
        this.author = author;
    }
}
