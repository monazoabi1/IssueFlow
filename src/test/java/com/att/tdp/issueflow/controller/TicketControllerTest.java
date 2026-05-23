package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.containsString;

import com.jayway.jsonpath.JsonPath;
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
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long projectId;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = createUser("ticketctrlowner", "ticketctrlowner@example.com");
        projectId = createProject("TicketCtrlProject-" + System.nanoTime(), ownerId);
    }

    /** Goal: POST /tickets with unknown projectId returns 404. */
    @Test
    void createTicket_unknownProject_returns404() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(99999L, null)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    /** Goal: POST /tickets with invalid body returns 400 Bad Request. */
    @Test
    void createTicket_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "",
                                  "status": "TODO",
                                  "priority": "HIGH",
                                  "type": "BUG",
                                  "projectId": %d
                                }
                                """.formatted(projectId)))
                .andExpect(status().isBadRequest());
    }

    /** Goal: POST /tickets with unknown assigneeId returns 404. */
    @Test
    void createTicket_unknownAssignee_returns404() throws Exception {
        mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(projectId, 99999L)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    /** Goal: GET /tickets/{id} for unknown ticket returns 404. */
    @Test
    void getTicketById_unknown_returns404() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    /** Goal: PATCH /tickets/{id} with invalid status transition returns 409. */
    @Test
    void updateTicket_invalidStatusTransition_returns409() throws Exception {
        long ticketId = createTicket(projectId, null);

        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0,
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Invalid status transition")));
    }

    /** Goal: PATCH /tickets/{id} without version field returns 400. */
    @Test
    void updateTicket_missingVersion_returns400() throws Exception {
        long ticketId = createTicket(projectId, null);

        // version is @NotNull on UpdateTicketRequest — validation runs before the service
        mockMvc.perform(patch("/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /** Goal: DELETE /tickets/{id} twice returns 409 on second attempt. */
    @Test
    void deleteTicket_twice_returns409() throws Exception {
        long ticketId = createTicket(projectId, null);

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)).andExpect(status().isOk());

        mockMvc.perform(delete("/tickets/{ticketId}", ticketId))
                .andExpect(status().isConflict())
                .andExpect(content().string("Ticket already deleted"));
    }

    /** Goal: GET /tickets/deleted without auth returns 401 Unauthorized. */
    @Test
    void getDeletedTickets_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/tickets/deleted").param("projectId", String.valueOf(projectId)))
                .andExpect(status().isUnauthorized());
    }

    /** Goal: Admin can list deleted tickets and restore one to active listing. */
    @Test
    void restoreTicket_asAdmin_makesTicketVisible() throws Exception {
        createUser("ticketadmin", "ticketadmin@example.com", "ADMIN");
        String token = login("ticketadmin", "secret");
        long ticketId = createTicket(projectId, null);
        mockMvc.perform(delete("/tickets/{ticketId}", ticketId)).andExpect(status().isOk());

        mockMvc.perform(get("/tickets/deleted")
                        .param("projectId", String.valueOf(projectId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ticketId + ")]").exists());

        mockMvc.perform(post("/tickets/{ticketId}/restore", ticketId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/tickets").param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + ticketId + ")]").exists());
    }

    /** Goal: GET /tickets by project excludes soft-deleted tickets. */
    @Test
    void getTicketsByProject_excludesDeletedTicket() throws Exception {
        long visibleId = createTicket(projectId, null);
        long deletedId = createTicket(projectId, null);
        mockMvc.perform(delete("/tickets/{ticketId}", deletedId)).andExpect(status().isOk());

        mockMvc.perform(get("/tickets").param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + visibleId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + deletedId + ")]").doesNotExist());
    }

    private long createUser(String username, String email) throws Exception {
        return createUser(username, email, "DEVELOPER");
    }

    private long createUser(String username, String email, String role) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "fullName": "Test User",
                                  "role": "%s",
                                  "password": "secret"
                                }
                                """.formatted(username, email, role)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.accessToken", String.class);
    }

    private long createProject(String name, long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Project for tickets",
                                  "ownerId": %d
                                }
                                """.formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createTicket(long projectId, Long assigneeId) throws Exception {
        MvcResult result = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ticketJson(projectId, assigneeId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private static String ticketJson(long projectId, Long assigneeId) {
        String assignee = assigneeId == null ? "null" : assigneeId.toString();
        return """
                {
                  "title": "Bug title",
                  "description": "Bug description",
                  "status": "TODO",
                  "priority": "HIGH",
                  "type": "BUG",
                  "projectId": %d,
                  "assigneeId": %s
                }
                """.formatted(projectId, assignee);
    }
}
