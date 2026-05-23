package com.att.tdp.issueflow.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TicketImportResponse {

    private int created;
    private int failed;
    private final List<String> errors = new ArrayList<>();

    public int getCreated() {
        return created;
    }

    public int getFailed() {
        return failed;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public void incrementCreated() {
        created++;
    }

    public void addError(String message) {
        failed++;
        errors.add(message);
    }
}
