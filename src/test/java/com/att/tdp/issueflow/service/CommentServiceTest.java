package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateCommentRequest;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private long ticketId;
    private long authorId;
    private String mentionUsername;

    @BeforeEach
    void setUp() {
        UserEntity owner = saveUser("commentowner", "commentowner@example.com");
        UserEntity author = saveUser("commentauthor", "commentauthor@example.com");
        mentionUsername = "mentioned";
        saveUser(mentionUsername, "mentioned@example.com");

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("CommentProject-" + System.nanoTime());
        projectRequest.setDescription("For comment tests");
        projectRequest.setOwnerId(owner.getId());
        long projectId = projectService.createProject(projectRequest).getId();

        CreateTicketRequest ticketRequest = new CreateTicketRequest();
        ticketRequest.setTitle("Comment ticket");
        ticketRequest.setDescription("Ticket body");
        ticketRequest.setStatus(TicketStatus.TODO);
        ticketRequest.setPriority(TicketPriority.MEDIUM);
        ticketRequest.setType(TicketType.BUG);
        ticketRequest.setProjectId(projectId);
        ticketId = ticketService.createTicket(ticketRequest).getTicketId();
        authorId = author.getId();
    }

    /** Goal: addComment on deleted ticket throws ResourceNotFoundException. */
    @Test
    void addComment_deletedTicket_throwsNotFound() {
        ticketService.deleteTicket(ticketId);

        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent("After ticket delete");

        assertThatThrownBy(() -> commentService.addComment(ticketId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found");
    }

    /** Goal: addComment on unknown ticket throws ResourceNotFoundException. */
    @Test
    void addComment_unknownTicket_throwsNotFound() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent("Hello");

        assertThatThrownBy(() -> commentService.addComment(99999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found");
    }

    /** Goal: addComment with unknown author throws ResourceNotFoundException. */
    @Test
    void addComment_unknownAuthor_throwsNotFound() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(99999L);
        request.setContent("Hello");

        assertThatThrownBy(() -> commentService.addComment(ticketId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    /** Goal: addComment with unknown @mention throws ConflictException. */
    @Test
    void addComment_unknownMention_throwsConflict() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent("Hello @ghost_user");

        assertThatThrownBy(() -> commentService.addComment(ticketId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Unknown mentioned user: @ghost_user");
    }

    /** Goal: addComment with valid mention persists comment, mentions, and CREATE audit log. */
    @Test
    void addComment_validMention_succeeds() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent("Hello @" + mentionUsername);

        var comment = commentService.addComment(ticketId, request);

        assertThat(comment.getCommentId()).isPositive();
        assertThat(comment.getContent()).contains("@" + mentionUsername);
        assertThat(commentService.getMentionedUsers(comment.getCommentId()))
                .hasSize(1)
                .first()
                .satisfies(m -> assertThat(m.getUsername()).isEqualTo(mentionUsername));
        assertThat(auditLogService.getAuditLogs(
                        EntityType.COMMENT, comment.getCommentId(), AuditAction.CREATE, null, authorId))
                .hasSize(1)
                .first()
                .satisfies(log -> {
                    assertThat(log.getPerformedBy()).isEqualTo(authorId);
                    assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
                });
    }

    /** Goal: getCommentsForTicket on unknown ticket throws ResourceNotFoundException. */
    @Test
    void getCommentsForTicket_unknownTicket_throwsNotFound() {
        assertThatThrownBy(() -> commentService.getCommentsForTicket(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found");
    }

    /** Goal: updateComment with wrong ticketId throws ResourceNotFoundException. */
    @Test
    void updateComment_wrongTicket_throwsNotFound() {
        long commentId = addSampleComment("Update me").getCommentId();

        UpdateCommentRequest update = new UpdateCommentRequest();
        update.setContent("Updated");

        assertThatThrownBy(() -> commentService.updateComment(99999L, commentId, update))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found for this ticket");
    }

    /** Goal: deleteComment with wrong ticketId throws ResourceNotFoundException. */
    @Test
    void deleteComment_wrongTicket_throwsNotFound() {
        long commentId = addSampleComment("Delete me").getCommentId();

        assertThatThrownBy(() -> commentService.deleteComment(99999L, commentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment not found for this ticket");
    }

    /** Goal: updateComment with unknown @mention throws ConflictException. */
    @Test
    void updateComment_unknownMention_throwsConflict() {
        long commentId = addSampleComment("Original").getCommentId();

        UpdateCommentRequest update = new UpdateCommentRequest();
        update.setContent("Now mentions @nobody_here");

        assertThatThrownBy(() -> commentService.updateComment(ticketId, commentId, update))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Unknown mentioned user: @nobody_here");
    }

    /** Goal: @mention matching username case-insensitively resolves the user. */
    @Test
    void addComment_caseInsensitiveMention_resolvesUser() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent("Hello @" + mentionUsername.toUpperCase());

        var comment = commentService.addComment(ticketId, request);

        assertThat(commentService.getMentionedUsers(comment.getCommentId()))
                .hasSize(1)
                .first()
                .satisfies(m -> assertThat(m.getUsername()).isEqualTo(mentionUsername));
    }

    /** Goal: updateComment re-parses content and updates mention list accordingly. */
    @Test
    void updateComment_reEvaluatesMentions() {
        long commentId = addSampleComment("Hello @" + mentionUsername).getCommentId();

        UpdateCommentRequest update = new UpdateCommentRequest();
        update.setContent("No mentions here");
        commentService.updateComment(ticketId, commentId, update);

        assertThat(commentService.getMentionedUsers(commentId)).isEmpty();

        update.setContent("Back @" + mentionUsername);
        commentService.updateComment(ticketId, commentId, update);

        assertThat(commentService.getMentionedUsers(commentId)).hasSize(1);
    }

    /** Goal: getMentionsForUser returns paginated comments ordered newest first. */
    @Test
    void getMentionsForUser_returnsCommentsNewestFirst() {
        addSampleComment("First @" + mentionUsername);
        addSampleComment("Second @" + mentionUsername);

        long mentionedUserId = userRepository.findByUsername(mentionUsername).orElseThrow().getId();
        var page = commentService.getMentionsForUser(mentionedUserId, 1, 10);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getData()).hasSize(2);
        assertThat(page.getData().get(0).getId()).isGreaterThan(page.getData().get(1).getId());
    }

    private com.att.tdp.issueflow.model.CommentEntity addSampleComment(String content) {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent(content);
        return commentService.addComment(ticketId, request);
    }

    private UserEntity saveUser(String username, String email) {
        UserEntity user = new UserEntity(username, email, username, Role.DEVELOPER);
        user.setPassword("secret");
        return userRepository.save(user);
    }
}
