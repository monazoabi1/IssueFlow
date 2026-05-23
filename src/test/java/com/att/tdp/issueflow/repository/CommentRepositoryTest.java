package com.att.tdp.issueflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.CommentEntity;
import com.att.tdp.issueflow.model.ProjectEntity;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@DataJpaTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {CommentRepository.class, TicketRepository.class, ProjectRepository.class, UserRepository.class}))
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private TicketEntity ticket;
    private UserEntity author;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("cowner", "cowner@example.com", "Comment Owner", Role.DEVELOPER);
        owner.setPassword("hash");
        ProjectEntity project = projectRepository.save(
                new ProjectEntity("Comment Project", "Desc", userRepository.save(owner)));
        ticket = ticketRepository.save(new TicketEntity(
                "Ticket", "Desc", TicketStatus.TODO, TicketPriority.LOW, TicketType.BUG, project.getId()));
        author = new UserEntity("author1", "author1@example.com", "Author One", Role.DEVELOPER);
        author.setPassword("hash");
        author = userRepository.save(author);
    }

    /** Goal: findByTicket_TicketId returns comments only for the requested ticket. */
    @Test
    void findByTicket_TicketId_returnsCommentsForTicketOnly() {
        CommentEntity onTicket = commentRepository.save(new CommentEntity("Hello", ticket, author));

        TicketEntity otherTicket = ticketRepository.save(new TicketEntity(
                "Other", "Desc", TicketStatus.TODO, TicketPriority.LOW, TicketType.BUG, ticket.getProjectId()));
        commentRepository.save(new CommentEntity("Other ticket comment", otherTicket, author));

        assertThat(commentRepository.findByTicket_TicketId(ticket.getTicketId()))
                .extracting(CommentEntity::getCommentId)
                .containsExactly(onTicket.getCommentId());
    }

    /** Goal: findByTicket_TicketId returns empty list when ticket has no comments. */
    @Test
    void findByTicket_TicketId_returnsEmptyWhenNoComments() {
        assertThat(commentRepository.findByTicket_TicketId(ticket.getTicketId())).isEmpty();
    }
}
