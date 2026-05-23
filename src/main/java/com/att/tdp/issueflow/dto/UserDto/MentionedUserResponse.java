package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.UserEntity;

public class MentionedUserResponse {

    private final long id;
    private final String username;
    private final String fullName;

    public MentionedUserResponse(UserEntity user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.fullName = user.getFullName();
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }
}
