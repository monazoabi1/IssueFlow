package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.ProjectEntity;
import jakarta.validation.constraints.NotBlank;

public class UpdateProjectRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    public void applyTo(ProjectEntity project) {
        project.setName(name);
        project.setDescription(description);
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
}
