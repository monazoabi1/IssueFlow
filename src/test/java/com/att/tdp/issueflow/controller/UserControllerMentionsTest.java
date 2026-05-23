package com.att.tdp.issueflow.controller;

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
class UserControllerMentionsTest {

    @Autowired
    private MockMvc mockMvc;

    private long mentionedUserId;
    private String mentionUsername;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = IntegrationTestSupport.createUser(mockMvc, "mentionowner", "mentionowner@example.com", "DEVELOPER");
        mentionUsername = "mentionuser";
        mentionedUserId = IntegrationTestSupport.createUser(mockMvc, mentionUsername, "mentionuser@example.com", "DEVELOPER");
        long authorId = IntegrationTestSupport.createUser(mockMvc, "mentionauthor", "mentionauthor@example.com", "DEVELOPER");
        long projectId = IntegrationTestSupport.createProject(mockMvc, "MentionProject-" + System.nanoTime(), ownerId);
        long ticketId = IntegrationTestSupport.createTicket(mockMvc, projectId, null);

        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authorId": %d,
                                  "content": "Hello @%s please review"
                                }
                                """.formatted(authorId, mentionUsername)))
                .andExpect(status().isOk());
    }

    /** Goal: GET /users/{id}/mentions returns paginated comments mentioning that user. */
    @Test
    void getMentionsForUser_returnsPagedComments() throws Exception {
        mockMvc.perform(get("/users/{userId}/mentions", mentionedUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.data[0].mentionedUsers[0].username").value(mentionUsername));
    }

    /** Goal: GET /users/{id}/mentions for unknown user returns 404. */
    @Test
    void getMentionsForUser_unknownUser_returns404() throws Exception {
        mockMvc.perform(get("/users/{userId}/mentions", 99999L)).andExpect(status().isNotFound());
    }
}
