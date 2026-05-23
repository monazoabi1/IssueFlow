package com.att.tdp.issueflow.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.att.tdp.issueflow.Exception.BadRequestException;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

class TicketCsvValidatorTest {

    private final TicketCsvValidator validator = new TicketCsvValidator();

    /** Goal: CSV headers missing required columns are rejected. */
    @Test
    void validateHeaders_missingColumn_rejects() throws Exception {
        assertThatThrownBy(() -> validator.validateHeaders(List.of("id", "title")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("description");
    }

    /** Goal: Rows with quoted commas and embedded quotes in description validate successfully. */
    @Test
    void validateRow_quotedCommasInDescription_succeeds() throws Exception {
        String csv = "id,title,description,status,priority,type,assigneeId\n"
                + "1,\"Comma title\",\"Says \"\"hello, world\"\"\",TODO,HIGH,BUG,\n";
        try (CSVParser parser = CSVParser.parse(
                csv,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            CSVRecord record = parser.iterator().next();
            assertDoesNotThrow(() -> validator.validateRow(record, 2));
        }
    }

    /** Goal: Rows with an invalid status value are rejected. */
    @Test
    void validateRow_invalidStatus_rejects() throws Exception {
        String csv = """
                id,title,description,status,priority,type,assigneeId
                1,Bug,Desc,INVALID,HIGH,BUG,
                """;
        try (CSVParser parser = CSVParser.parse(
                csv,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
            CSVRecord record = parser.iterator().next();
            assertThatThrownBy(() -> validator.validateRow(record, 2))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("invalid status");
        }
    }
}
