package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String fullName;

    @NotNull
    private UserEntity.Role role;

    @NotBlank
    private String password;

    public UserEntity toEntity() {
        UserEntity user = new UserEntity(username, email, fullName, role);
        user.setPassword(password);
        return user;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}