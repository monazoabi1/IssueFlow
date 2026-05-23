package com.att.tdp.issueflow.dto;

public class WorkloadEntryResponse {

    private final long userId;
    private final String username;
    private final long openTicketCount;

    public WorkloadEntryResponse(long userId, String username, long openTicketCount) {
        this.userId = userId;
        this.username = username;
        this.openTicketCount = openTicketCount;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public long getOpenTicketCount() {
        return openTicketCount;
    }
}
