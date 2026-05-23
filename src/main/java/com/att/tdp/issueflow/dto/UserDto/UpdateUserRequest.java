package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRequest {

    @NotBlank
    private String fullName;

    @NotNull
    private UserEntity.Role role;

    public UserEntity toEntity() {
        UserEntity user = new UserEntity();
        user.setFullName(fullName);
        user.setRole(role);
        return user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserEntity.Role getRole() {
        return role;
    }

    public void setRole(UserEntity.Role role) {
        this.role = role;
    }
}
