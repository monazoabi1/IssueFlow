package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long ticketId;
    private long authorId;
    private String mentionUsername;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = createUser("commentctrlowner", "commentctrlowner@example.com");
        mentionUsername = "mentiontarget";
        createUser(mentionUsername, "mentiontarget@example.com");
        authorId = createUser("commentauthor", "commentauthor@example.com");
        long projectId = createProject("CommentCtrlProject-" + System.nanoTime(), ownerId);
        ticketId = createTicket(projectId);
    }

    /** Goal: POST comment on unknown ticket returns 404. */
    @Test
    void createComment_unknownTicket_returns404() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(authorId, "Hello")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    /** Goal: POST comment with unknown authorId returns 404. */
    @Test
    void createComment_unknownAuthor_returns404() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(99999L, "Hello")))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    /** Goal: POST comment with unknown @mention returns 409 Conflict. */
    @Test
    void createComment_unknownMention_returns409() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(authorId, "Hi @unknown_user_xyz")))
                .andExpect(status().isConflict())
                .andExpect(content().string("Unknown mentioned user: @unknown_user_xyz"));
    }

    /** Goal: POST comment with valid @mention returns mentionedUsers in response. */
    @Test
    void createComment_validMention_returnsMentionedUsers() throws Exception {
        mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(authorId, "Hello @" + mentionUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mentionedUsers[0].username").value(mentionUsername));
    }

    /** Goal: GET comments for unknown ticket returns 404. */
    @Test
    void getComments_unknownTicket_returns404() throws Exception {
        mockMvc.perform(get("/tickets/{ticketId}/comments", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    /** Goal: PATCH comment under wrong ticket path returns 404. */
    @Test
    void updateComment_wrongTicket_returns404() throws Exception {
        long commentId = createComment(ticketId, authorId, "Original");

        mockMvc.perform(patch("/tickets/{ticketId}/comments/{commentId}", 99999L, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Updated"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Comment not found for this ticket"));
    }

    /** Goal: DELETE comment under wrong ticket path returns 404. */
    @Test
    void deleteComment_wrongTicket_returns404() throws Exception {
        long commentId = createComment(ticketId, authorId, "To delete");

        mockMvc.perform(delete("/tickets/{ticketId}/comments/{commentId}", 99999L, commentId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Comment not found for this ticket"));
    }

    /** Goal: PATCH comment with unknown @mention returns 409 Conflict. */
    @Test
    void updateComment_unknownMention_returns409() throws Exception {
        long commentId = createComment(ticketId, authorId, "Original");

        mockMvc.perform(patch("/tickets/{ticketId}/comments/{commentId}", ticketId, commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Bad @ghost_mention"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string("Unknown mentioned user: @ghost_mention"));
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
                                """.formatted(username, email)))
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
                                  "description": "For comments",
                                  "ownerId": %d
                                }
                                """.formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createTicket(long projectId) throws Exception {
        MvcResult result = mockMvc.perform(post("/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ticket",
                                  "description": "Desc",
                                  "status": "TODO",
                                  "priority": "MEDIUM",
                                  "type": "BUG",
                                  "projectId": %d
                                }
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createComment(long ticketId, long authorId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(authorId, content)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private static String commentJson(long authorId, String content) {
        return """
                {
                  "authorId": %d,
                  "content": "%s"
                }
                """.formatted(authorId, content);
    }
}
