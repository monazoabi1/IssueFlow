package com.att.tdp.issueflow.validation;

import com.att.tdp.issueflow.Exception.BadRequestException;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class TicketCsvValidator {

    public static final List<String> REQUIRED_HEADERS =
            List.of("id", "title", "description", "status", "priority", "type", "assigneeId");

    public void validateHeaders(List<String> headers) {
        if (headers == null || headers.isEmpty()) {
            throw new BadRequestException("CSV header row is required");
        }
        for (String required : REQUIRED_HEADERS) {
            if (!headers.contains(required)) {
                throw new BadRequestException("CSV missing required column: " + required);
            }
        }
    }

    public void validateRow(CSVRecord record, int rowNumber) {
        requireNonBlank(record, "title", rowNumber);
        requireNonBlank(record, "description", rowNumber);
        requireNonBlank(record, "status", rowNumber);
        requireNonBlank(record, "priority", rowNumber);
        requireNonBlank(record, "type", rowNumber);
        parseStatus(record.get("status"), rowNumber);
        parsePriority(record.get("priority"), rowNumber);
        parseType(record.get("type"), rowNumber);
        String assignee = record.get("assigneeId");
        if (assignee != null && !assignee.isBlank() && !assignee.trim().matches("\\d+")) {
            throw new BadRequestException("Row " + rowNumber + ": assigneeId must be a number");
        }
    }

    public TicketStatus parseStatus(String value, int rowNumber) {
        try {
            return TicketStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Row " + rowNumber + ": invalid status '" + value + "'");
        }
    }

    public TicketPriority parsePriority(String value, int rowNumber) {
        try {
            return TicketPriority.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Row " + rowNumber + ": invalid priority '" + value + "'");
        }
    }

    public TicketType parseType(String value, int rowNumber) {
        try {
            return TicketType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Row " + rowNumber + ": invalid type '" + value + "'");
        }
    }

    public Long parseAssigneeId(CSVRecord record) {
        String raw = record.get("assigneeId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Long.parseLong(raw.trim());
    }

    private void requireNonBlank(CSVRecord record, String column, int rowNumber) {
        if (!record.isMapped(column) || record.get(column) == null || record.get(column).isBlank()) {
            throw new BadRequestException("Row " + rowNumber + ": " + column + " is required");
        }
    }
}
