package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.att.tdp.issueflow.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerFeatureTest {

    @Autowired
    private MockMvc mockMvc;

    /** Goal: GET /projects/{id}/workload returns developers with correct open ticket counts. */
    @Test
    void getWorkload_returnsDevelopersSortedByOpenTickets() throws Exception {
        long ownerId = IntegrationTestSupport.createUser(mockMvc, "wloadowner", "wloadowner@example.com", "ADMIN");
        long devLight = IntegrationTestSupport.createUser(mockMvc, "devlight", "devlight@example.com", "DEVELOPER");
        long devHeavy = IntegrationTestSupport.createUser(mockMvc, "devheavy", "devheavy@example.com", "DEVELOPER");
        long projectId = IntegrationTestSupport.createProject(mockMvc, "WorkloadCtrl-" + System.nanoTime(), ownerId);

        IntegrationTestSupport.createTicket(mockMvc, projectId, devHeavy);
        IntegrationTestSupport.createTicket(mockMvc, projectId, devHeavy);

        mockMvc.perform(get("/projects/{projectId}/workload", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'devlight')].openTicketCount").value(0))
                .andExpect(jsonPath("$[?(@.username == 'devheavy')].openTicketCount").value(2));
    }

    /** Goal: Admin can soft-delete a project, list it among deleted, and restore it. */
    @Test
    void softDeleteRestore_asAdmin() throws Exception {
        long adminId = IntegrationTestSupport.createUser(mockMvc, "projadmin", "projadmin@example.com", "ADMIN");
        long projectId = IntegrationTestSupport.createProject(mockMvc, "RestoreCtrl-" + System.nanoTime(), adminId);
        String token = IntegrationTestSupport.login(mockMvc, "projadmin", "secret");

        mockMvc.perform(delete("/projects/{id}", projectId)).andExpect(status().isOk());

        mockMvc.perform(get("/projects/deleted").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + projectId + ")]").exists());

        mockMvc.perform(post("/projects/{id}/restore", projectId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId));
    }

    /** Goal: GET /projects/deleted without auth returns 401 Unauthorized. */
    @Test
    void getDeletedProjects_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/projects/deleted")).andExpect(status().isUnauthorized());
    }
}
