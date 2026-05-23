package com.att.tdp.issueflow.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Shared HTTP helpers for controller and stress tests. */
public final class IntegrationTestSupport {

    private IntegrationTestSupport() {}

    public static long createUser(MockMvc mockMvc, String username, String email, String role)
            throws Exception {
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

    public static long createProject(MockMvc mockMvc, String name, long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(post("/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "description": "Integration test project",
                                  "ownerId": %d
                                }
                                """.formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    public static long createTicket(MockMvc mockMvc, long projectId, Long assigneeId) throws Exception {
        String assigneeJson = assigneeId == null ? "null" : assigneeId.toString();
        MvcResult result = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Test ticket",
                                  "description": "Test description",
                                  "status": "TODO",
                                  "priority": "MEDIUM",
                                  "type": "BUG",
                                  "projectId": %d,
                                  "assigneeId": %s
                                }
                                """.formatted(projectId, assigneeJson)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    public static String login(MockMvc mockMvc, String username, String password) throws Exception {
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
}
