package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.UserEntity;

public class UserResponse {

    private final long id;
    private final String username;
    private final String email;
    private final String fullName;
    private final UserEntity.Role role;

    public UserResponse(UserEntity user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.role = user.getRole();
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public UserEntity.Role getRole() {
        return role;
    }
}
