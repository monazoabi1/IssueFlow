package com.att.tdp.issueflow.stress;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.CreateCommentRequest;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.service.CommentService;
import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.service.TicketService;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("stress")
@SpringBootTest
@Transactional
class CommentMentionStressTest {

    private static final int MENTION_USER_COUNT = 15;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    private long ticketId;
    private long authorId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("mentionstress", "mentionstress@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        UserEntity author = new UserEntity("mentionauthor", "mentionauthor@example.com", "Author", Role.DEVELOPER);
        author.setPassword("secret");
        authorId = userRepository.save(author).getId();

        for (int i = 0; i < MENTION_USER_COUNT; i++) {
            UserEntity mentioned = new UserEntity("muser" + i, "muser" + i + "@example.com", "Mentioned " + i, Role.DEVELOPER);
            mentioned.setPassword("secret");
            userRepository.save(mentioned);
        }

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("MentionStress-" + System.nanoTime());
        projectRequest.setDescription("Mention stress");
        projectRequest.setOwnerId(owner.getId());
        long projectId = projectService.createProject(projectRequest).getId();

        CreateTicketRequest ticketRequest = new CreateTicketRequest();
        ticketRequest.setTitle("Mention ticket");
        ticketRequest.setDescription("Body");
        ticketRequest.setStatus(TicketStatus.TODO);
        ticketRequest.setPriority(TicketPriority.MEDIUM);
        ticketRequest.setType(TicketType.BUG);
        ticketRequest.setProjectId(projectId);
        ticketId = ticketService.createTicket(ticketRequest).getTicketId();
    }

    /** Goal: Comment with many @mentions persists all mentioned users. */
    @Test
    void addComment_manyMentions_persistsAll() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < MENTION_USER_COUNT; i++) {
            content.append("@muser").append(i).append(' ');
        }

        CreateCommentRequest request = new CreateCommentRequest();
        request.setAuthorId(authorId);
        request.setContent(content.toString());

        var comment = commentService.addComment(ticketId, request);

        assertThat(commentService.getMentionedUsers(comment.getCommentId())).hasSize(MENTION_USER_COUNT);
    }
}
