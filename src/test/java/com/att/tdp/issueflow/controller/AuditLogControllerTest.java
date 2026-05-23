package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
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
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long ownerId;
    private long projectId;

    @BeforeEach
    void setUp() throws Exception {
        ownerId = createUser("auditowner", "auditowner@example.com");
        projectId = createProject("AuditProject-" + System.nanoTime(), ownerId);
    }

    /** Goal: GET /audit-logs includes CREATE, UPDATE, and DELETE entries for a project. */
    @Test
    void getAuditLogs_returnsCreateUpdateDeleteForProject() throws Exception {
        mockMvc.perform(patch("/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Renamed",
                                  "description": "Renamed description"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/projects/{id}", projectId)).andExpect(status().isOk());

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                                "$[?(@.entityType == 'PROJECT' && @.entityId == %d && @.action == 'CREATE')]"
                                        .formatted(projectId))
                        .exists())
                .andExpect(jsonPath(
                                "$[?(@.entityType == 'PROJECT' && @.entityId == %d && @.action == 'UPDATE')]"
                                        .formatted(projectId))
                        .exists())
                .andExpect(jsonPath(
                                "$[?(@.entityType == 'PROJECT' && @.entityId == %d && @.action == 'DELETE')]"
                                        .formatted(projectId))
                        .exists())
                .andExpect(jsonPath("$[?(@.actor == 'USER')]").exists());
    }

    /** Goal: GET /audit-logs filtered by entityType and action returns matching entries only. */
    @Test
    void getAuditLogs_filterByEntityTypeAndAction() throws Exception {
        mockMvc.perform(get("/audit-logs")
                        .param("entityType", "PROJECT")
                        .param("action", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entityType != 'PROJECT')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.action != 'CREATE')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.entityId == %d)]".formatted(projectId)).exists());
    }

    /** Goal: GET /audit-logs filtered by entityId returns only that entity's logs. */
    @Test
    void getAuditLogs_filterByEntityId() throws Exception {
        mockMvc.perform(get("/audit-logs").param("entityId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.entityId != %d)]".formatted(projectId)).doesNotExist());
    }

    /** Goal: GET /audit-logs filtered by actor returns only matching actor entries. */
    @Test
    void getAuditLogs_filterByActor() throws Exception {
        mockMvc.perform(get("/audit-logs").param("actor", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.actor != 'USER')]").doesNotExist());
    }

    /** Goal: GET /audit-logs filtered by performedBy returns only that user's actions. */
    @Test
    void getAuditLogs_filterByPerformedBy() throws Exception {
        long actorId = createUser("performer", "performer@example.com", "secret123");
        String token = loginAndGetToken("performer", "secret123");

        mockMvc.perform(patch("/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Filtered performer",
                                  "description": "Update for performedBy filter"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit-logs").param("performedBy", String.valueOf(actorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.performedBy != %d)]".formatted(actorId)).doesNotExist())
                .andExpect(jsonPath(
                                "$[?(@.entityType == 'PROJECT' && @.entityId == %d && @.action == 'UPDATE')]"
                                        .formatted(projectId))
                        .exists());
    }

    /** Goal: Authenticated project update records performedBy in audit log response. */
    @Test
    void getAuditLogs_authenticatedUpdate_recordsPerformedBy() throws Exception {
        long actorId = createUser("auditactor", "auditactor@example.com", "secret123");
        String token = loginAndGetToken("auditactor", "secret123");

        mockMvc.perform(patch("/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Auth Renamed",
                                  "description": "Updated with JWT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/audit-logs")
                        .param("entityType", "PROJECT")
                        .param("entityId", String.valueOf(projectId))
                        .param("action", "UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("UPDATE"))
                .andExpect(jsonPath("$[0].performedBy").value(actorId));
    }

    /** Goal: GET /audit-logs returns entries sorted newest-first by timestamp. */
    @Test
    void getAuditLogs_returnsNewestFirst() throws Exception {
        mockMvc.perform(patch("/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Older change",
                                  "description": "First update"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/projects/{id}", projectId)).andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/audit-logs")
                        .param("entityType", "PROJECT")
                        .param("entityId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        List<String> timestamps = JsonPath.read(result.getResponse().getContentAsString(), "$[*].timestamp");
        for (int i = 0; i < timestamps.size() - 1; i++) {
            Instant newer = Instant.parse(timestamps.get(i));
            Instant older = Instant.parse(timestamps.get(i + 1));
            assertThat(newer).isAfterOrEqualTo(older);
        }
        List<String> actions = JsonPath.read(result.getResponse().getContentAsString(), "$[*].action");
        assertThat(actions).containsExactlyInAnyOrder("CREATE", "UPDATE", "DELETE");
    }

    private long createUser(String username, String email) throws Exception {
        return createUser(username, email, "secret");
    }

    private long createUser(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "email": "%s",
                                  "fullName": "Test User",
                                  "role": "DEVELOPER",
                                  "password": "%s"
                                }
                                """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createProject(String name, long projectOwnerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Audit test project",
                                  "ownerId": %d
                                }
                                """.formatted(name, projectOwnerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
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
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }
}
