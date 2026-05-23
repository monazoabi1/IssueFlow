package com.att.tdp.issueflow.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketExportImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long projectId;
    private long assigneeId;

    @BeforeEach
    void setUp() throws Exception {
        assigneeId = createUser("csvctrl", "csvctrl@example.com");
        projectId = createProject("CsvCtrlProject-" + System.nanoTime(), assigneeId);
        createTicket(projectId, "Seed ticket", "Description with, comma");
    }

    /** Goal: GET /tickets/export returns CSV with headers and project tickets. */
    @Test
    void exportTickets_returnsCsvWithHeaders() throws Exception {
        mockMvc.perform(get("/tickets/export").param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("tickets-" + projectId + ".csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("id,title,description,status,priority,type,assigneeId")))
                .andExpect(content().string(containsString("Seed ticket")));
    }

    /** Goal: POST /tickets/import with valid CSV returns created count and no errors. */
    @Test
    void importTickets_validCsv_returnsSummary() throws Exception {
        String csv = """
                id,title,description,status,priority,type,assigneeId
                ,Imported,From CSV,TODO,HIGH,BUG,%d
                """
                .formatted(assigneeId);
        MockMultipartFile file =
                new MockMultipartFile("file", "tickets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/tickets/import")
                        .file(file)
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.failed").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    /** Goal: POST /tickets/import with mixed rows reports partial success and row errors. */
    @Test
    void importTickets_partialFailure_returnsErrors() throws Exception {
        String csv = """
                id,title,description,status,priority,type,assigneeId
                ,Good,Valid,TODO,HIGH,BUG,
                ,Bad,Invalid status,NOT_A_STATUS,HIGH,BUG,
                """;
        MockMultipartFile file =
                new MockMultipartFile("file", "tickets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/tickets/import")
                        .file(file)
                        .param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.errors[0]").value(containsString("invalid status")));
    }

    /** Goal: GET /tickets/export for unknown project returns 404. */
    @Test
    void exportTickets_unknownProject_returns404() throws Exception {
        mockMvc.perform(get("/tickets/export").param("projectId", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    private long createUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "fullName": "Test User",
                                  "role": "DEVELOPER",
                                  "password": "secret"
                                }
                                """
                                .formatted(username, email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createProject(String name, long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "CSV controller tests",
                                  "ownerId": %d
                                }
                                """
                                .formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private void createTicket(long projectId, String title, String description) throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "type": "BUG",
                                  "projectId": %d
                                }
                                """
                                .formatted(title, description, projectId)))
                .andExpect(status().isOk());
    }
}
