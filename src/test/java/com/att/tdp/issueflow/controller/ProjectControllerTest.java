package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Goal: POST /projects returns 200 with project fields in JSON body. */
    @Test
    void createProject_returns200AndProjectResponse() throws Exception {
        long ownerId = createUserAndGetId("projowner", "projowner@example.com");

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson("Alpha", ownerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Alpha"))
                .andExpect(jsonPath("$.description").value("A sample project"))
                .andExpect(jsonPath("$.ownerId").value(ownerId));
    }

    /** Goal: POST /projects with unknown ownerId returns 404. */
    @Test
    void createProject_unknownOwner_returns404() throws Exception {
        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson("Orphan", 99999L)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    /** Goal: POST /projects with invalid body returns 400 Bad Request. */
    @Test
    void createProject_invalidBody_returns400() throws Exception {
        long ownerId = createUserAndGetId("invalidowner", "invalidowner@example.com");

        mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "",
                                  "ownerId": %d
                                }
                                """.formatted(ownerId)))
                .andExpect(status().isBadRequest());
    }

    /** Goal: GET /projects/{id} returns 200 with project details. */
    @Test
    void getProjectById_returns200() throws Exception {
        long ownerId = createUserAndGetId("getter", "getter@example.com");
        long projectId = createProjectAndGetId("Beta", ownerId);

        mockMvc.perform(get("/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Beta"));
    }

    /** Goal: GET /projects/{id} for unknown id returns 404. */
    @Test
    void getProjectById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/projects/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    /** Goal: GET /projects includes newly created project in list. */
    @Test
    void getAllProjects_returnsListIncludingCreatedProject() throws Exception {
        long ownerId = createUserAndGetId("lister", "lister@example.com");
        createProjectAndGetId("Gamma", ownerId);

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Gamma')]").exists());
    }

    /** Goal: PATCH /projects/{id} returns 200 with updated name and description. */
    @Test
    void updateProject_returns200WithUpdatedFields() throws Exception {
        long ownerId = createUserAndGetId("-patcher", "patcher@example.com");
        long projectId = createProjectAndGetId("Delta", ownerId);

        mockMvc.perform(patch("/projects/{id}", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Delta Updated",
                                  "description": "Updated project description"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Delta Updated"))
                .andExpect(jsonPath("$.description").value("Updated project description"));
    }

    /** Goal: DELETE /projects/{id} succeeds; subsequent GET returns 404. */
    @Test
    void deleteProject_returns200ThenGetReturns404() throws Exception {
        long ownerId = createUserAndGetId("deleter", "deleter@example.com");
        long projectId = createProjectAndGetId("Epsilon", ownerId);

        mockMvc.perform(delete("/projects/{id}", projectId)).andExpect(status().isOk());

        mockMvc.perform(get("/projects/{id}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Project not found"));
    }

    /** Goal: DELETE /projects/{id} twice returns 409 on second attempt. */
    @Test
    void deleteProject_twice_returns409() throws Exception {
        long ownerId = createUserAndGetId("twicedel", "twicedel@example.com");
        long projectId = createProjectAndGetId("Zeta", ownerId);

        mockMvc.perform(delete("/projects/{id}", projectId)).andExpect(status().isOk());

        mockMvc.perform(delete("/projects/{id}", projectId))
                .andExpect(status().isConflict())
                .andExpect(content().string("Project already deleted"));
    }

    private long createUserAndGetId(String username, String email) throws Exception {
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
                                """.formatted(username, email)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createProjectAndGetId(String name, long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private static String createProjectJson(String name, long ownerId) {
        return """
                {
                  "name": "%s",
                  "description": "A sample project",
                  "ownerId": %d
                }
                """.formatted(name, ownerId);
    }
}
