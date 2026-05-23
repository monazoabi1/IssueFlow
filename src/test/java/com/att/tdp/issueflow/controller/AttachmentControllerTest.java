package com.att.tdp.issueflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private long ticketId;

    @BeforeEach
    void setUp() throws Exception {
        long ownerId = createUser("attachowner", "attachowner@example.com");
        long projectId = createProject("AttachProject-" + System.nanoTime(), ownerId);
        ticketId = createTicket(projectId);
    }

    /** Goal: POST attachment with allowed PNG returns metadata JSON. */
    @Test
    void uploadAttachment_allowedPng_returnsMetadata() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "screenshot.png", "image/png", new byte[] {1, 2, 3, 4});

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketId").value(ticketId))
                .andExpect(jsonPath("$.filename").value("screenshot.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    /** Goal: POST attachment with disallowed type returns 400 with allowed-types message. */
    @Test
    void uploadAttachment_disallowedType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "archive.zip", "application/zip", new byte[] {1, 2});

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Attachment type not allowed. Allowed: image/png, image/jpeg, application/pdf, text/plain"));
    }

    /** Goal: POST attachment for unknown ticket returns 404. */
    @Test
    void uploadAttachment_unknownTicket_returns404() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/tickets/{ticketId}/attachments", 99999L).file(file))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    /** Goal: DELETE existing attachment succeeds; second delete returns 404. */
    @Test
    void deleteAttachment_existing_returnsOk() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "log.txt", "text/plain", "log line".getBytes());

        MvcResult upload = mockMvc.perform(multipart("/tickets/{ticketId}/attachments", ticketId).file(file))
                .andExpect(status().isOk())
                .andReturn();
        long attachmentId =
                JsonPath.parse(upload.getResponse().getContentAsString()).read("$.id", Long.class);

        mockMvc.perform(delete("/tickets/{ticketId}/attachments/{attachmentId}", ticketId, attachmentId))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/tickets/{ticketId}/attachments/{attachmentId}", ticketId, attachmentId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Attachment not found"));
    }

    private long createUser(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "username": "%s",
                                          "email": "%s",
                                          "fullName": "Test User",
                                          "role": "DEVELOPER",
                                          "password": "secret"
                                        }
                                        """
                                        .formatted(username, email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createProject(String name, long ownerId) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/projects")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "%s",
                                          "description": "Attachment tests",
                                          "ownerId": %d
                                        }
                                        """
                                        .formatted(name, ownerId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }

    private long createTicket(long projectId) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Attachment ticket",
                                          "description": "For attachments",
                                          "status": "TODO",
                                          "priority": "HIGH",
                                          "type": "BUG",
                                          "projectId": %d
                                        }
                                        """
                                        .formatted(projectId)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
    }
}
