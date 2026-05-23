package com.att.tdp.issueflow.dto;

import com.att.tdp.issueflow.model.ProjectEntity;

public class ProjectResponse {

    private final long id;
    private final String name;
    private final String description;
    private final long ownerId;

    public ProjectResponse(ProjectEntity project) {
        this.id = project.getId();
        this.name = project.getName();
        this.description = project.getDescription();
        this.ownerId = project.getOwner().getId();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getOwnerId() {
        return ownerId;
    }
}
