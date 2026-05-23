package com.att.tdp.issueflow.dto;

import java.util.List;

public class MentionsPageResponse {

    private final List<CommentResponse> data;
    private final long total;
    private final int page;

    public MentionsPageResponse(List<CommentResponse> data, long total, int page) {
        this.data = data;
        this.total = total;
        this.page = page;
    }

    public List<CommentResponse> getData() {
        return data;
    }

    public long getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }
}
