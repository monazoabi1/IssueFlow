package com.att.tdp.issueflow.dto;

import jakarta.validation.constraints.NotNull;

public class AddDependencyRequest {

    @NotNull
    private Long blockedBy;

    public Long getBlockedBy() {
        return blockedBy;
    }

    public void setBlockedBy(Long blockedBy) {
        this.blockedBy = blockedBy;
    }
}
