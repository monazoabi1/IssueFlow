package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.support.IntegrationTestSupport;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketEscalationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long projectId;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = IntegrationTestSupport.createUser(mockMvc, "escctrlowner", "escctrlowner@example.com", "DEVELOPER");
        projectId = IntegrationTestSupport.createProject(mockMvc, "EscCtrl-" + System.nanoTime(), ownerId);
    }

    /** Goal: GET /tickets/{id} for overdue critical ticket includes isOverdue true. */
    @Test
    void getTicket_overdueCritical_showsIsOverdueFlag() throws Exception {
        MvcResult create = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Overdue critical",
                                  "description": "Body",
                                  "status": "TODO",
                                  "priority": "CRITICAL",
                                  "type": "BUG",
                                  "projectId": %d,
                                  "dueDate": "%s"
                                }
                                """.formatted(projectId, Instant.now().minusSeconds(120).toString())))
                .andExpect(status().isOk())
                .andReturn();

        long ticketId = JsonPath.parse(create.getResponse().getContentAsString()).read("$.id", Long.class);

        mockMvc.perform(get("/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.isOverdue").value(true));
    }
}
