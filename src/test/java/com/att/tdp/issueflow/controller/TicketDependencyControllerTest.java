package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketDependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long projectId;
    private long ticketId;
    private long blockerId;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = IntegrationTestSupport.createUser(mockMvc, "depctrlowner", "depctrlowner@example.com", "DEVELOPER");
        projectId = IntegrationTestSupport.createProject(mockMvc, "DepCtrl-" + System.nanoTime(), ownerId);
        ticketId = IntegrationTestSupport.createTicket(mockMvc, projectId, null);
        blockerId = IntegrationTestSupport.createTicket(mockMvc, projectId, null);
    }

    /** Goal: POST then GET /tickets/{id}/dependencies returns the blocker ticket. */
    @Test
    void addAndListDependency() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedBy": %d }
                                """.formatted(blockerId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}/dependencies", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockerId))
                .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    /** Goal: DELETE dependency removes it from the ticket's blocker list. */
    @Test
    void removeDependency() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedBy": %d }
                                """.formatted(blockerId)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/tickets/{ticketId}/dependencies/{blockerId}", ticketId, blockerId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets/{ticketId}/dependencies", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    /** Goal: Adding a circular dependency returns 409 Conflict. */
    @Test
    void addDependency_circular_returns409() throws Exception {
        long ticketA = IntegrationTestSupport.createTicket(mockMvc, projectId, null);
        long ticketB = IntegrationTestSupport.createTicket(mockMvc, projectId, null);

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedBy": %d }
                                """.formatted(ticketB)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/tickets/{ticketId}/dependencies", ticketB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "blockedBy": %d }
                                """.formatted(ticketA)))
                .andExpect(status().isConflict());
    }
}
