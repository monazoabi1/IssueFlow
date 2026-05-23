package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /** Goal: POST /users returns 200 with user fields and no password in response. */
    @Test
    void createUser_returns200AndUserResponseWithoutPassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson("jdoe", "jdoe@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value("jdoe"))
                .andExpect(jsonPath("$.email").value("jdoe@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /** Goal: POST /users with invalid body returns 400 Bad Request. */
    @Test
    void createUser_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "fullName": "",
                                  "role": "DEVELOPER",
                                  "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /** Goal: POST /users with duplicate username returns 409 Conflict. */
    @Test
    void createUser_duplicateUsername_returns409() throws Exception {
        String body = createUserJson("duplicate", "first@example.com");
        mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson("duplicate", "second@example.com")))
                .andExpect(status().isConflict())
                .andExpect(content().string("Username already exists"));
    }

    /** Goal: GET /users/{id} returns 200 with user details. */
    @Test
    void getUserById_returns200() throws Exception {
        long id = createUserAndGetId("alice", "alice@example.com");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    /** Goal: GET /users/{id} for unknown id returns 404. */
    @Test
    void getUserById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/users/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    /** Goal: GET /users includes newly created user in list. */
    @Test
    void getAllUsers_returnsListIncludingCreatedUser() throws Exception {
        createUserAndGetId("bob", "bob@example.com");

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'bob')]").exists());
    }

    /** Goal: PUT /users/{id} returns 200 with updated fullName and role. */
    @Test
    void updateUser_returns200WithUpdatedFields() throws Exception {
        long id = createUserAndGetId("carol", "carol@example.com");

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Carol Updated",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.fullName").value("Carol Updated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    /** Goal: DELETE /users/{id} succeeds; subsequent GET returns 404. */
    @Test
    void deleteUser_returns200ThenGetReturns404() throws Exception {
        long id = createUserAndGetId("dave", "dave@example.com");

        mockMvc.perform(delete("/users/{id}", id)).andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    private long createUserAndGetId(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson(username, email)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private static String createUserJson(String username, String email) {
        return """
                {
                  "username": "%s",
                  "email": "%s",
                  "fullName": "John Doe",
                  "role": "DEVELOPER",
                  "password": "secret"
                }
                """.formatted(username, email);
    }
}
