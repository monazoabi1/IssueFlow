package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.ProjectEntity;
import com.att.tdp.issueflow.model.UserEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateProjectRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private Long ownerId;

    public ProjectEntity toEntity(UserEntity owner) {
        return new ProjectEntity(name, description, owner);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }
}
