package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.CommentEntity;
import java.util.Collections;
import java.util.List;

public class CommentResponse {

    private final long id;
    private final long ticketId;
    private final long authorId;
    private final String content;
    private final List<MentionedUserResponse> mentionedUsers;

    public CommentResponse(CommentEntity comment, List<MentionedUserResponse> mentionedUsers) {
        this.id = comment.getCommentId();
        this.ticketId = comment.getTicket().getTicketId();
        this.authorId = comment.getAuthor().getId();
        this.content = comment.getContent();
        this.mentionedUsers = mentionedUsers == null ? Collections.emptyList() : mentionedUsers;
    }

    public long getId() {
        return id;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getAuthorId() {
        return authorId;
    }

    public String getContent() {
        return content;
    }

    public List<MentionedUserResponse> getMentionedUsers() {
        return mentionedUsers;
    }
}
