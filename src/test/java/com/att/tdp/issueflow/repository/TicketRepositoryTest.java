package com.att.tdp.issueflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
        classes = {TicketRepository.class, ProjectRepository.class, UserRepository.class}))
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("towner", "towner@example.com", "Ticket Owner", Role.DEVELOPER);
        owner.setPassword("hash");
        ProjectEntity project = projectRepository.save(
                new ProjectEntity("Ticket Project", "For ticket repo tests", userRepository.save(owner)));
        projectId = project.getId();
    }

    /** Goal: findByTicketIdAndIsDeletedFalse returns active ticket by id. */
    @Test
    void findByTicketIdAndIsDeletedFalse_returnsActiveTicket() {
        TicketEntity saved = ticketRepository.save(sampleTicket("Active ticket"));

        assertThat(ticketRepository.findByTicketIdAndIsDeletedFalse(saved.getTicketId()))
                .isPresent()
                .get()
                .extracting(TicketEntity::getTitle)
                .isEqualTo("Active ticket");
    }

    /** Goal: findByTicketIdAndIsDeletedFalse returns empty for soft-deleted ticket. */
    @Test
    void findByTicketIdAndIsDeletedFalse_returnsEmptyWhenSoftDeleted() {
        TicketEntity ticket = ticketRepository.save(sampleTicket("Deleted ticket"));
        ticket.setDeleted(true);
        ticketRepository.save(ticket);

        assertThat(ticketRepository.findByTicketIdAndIsDeletedFalse(ticket.getTicketId())).isEmpty();
    }

    /** Goal: findAllByProjectIdAndIsDeletedFalse excludes deleted and other-project tickets. */
    @Test
    void findAllByProjectIdAndIsDeletedFalse_filtersProjectAndDeletedFlag() {
        TicketEntity active = ticketRepository.save(sampleTicket("Visible"));
        TicketEntity deleted = ticketRepository.save(sampleTicket("Hidden"));
        deleted.setDeleted(true);
        ticketRepository.save(deleted);

        TicketEntity otherProject = ticketRepository.save(
                new TicketEntity("Other", "Other project", TicketStatus.TODO, TicketPriority.LOW, TicketType.BUG, 999L));

        assertThat(ticketRepository.findAllByProjectIdAndIsDeletedFalse(projectId))
                .extracting(TicketEntity::getTicketId)
                .contains(active.getTicketId())
                .doesNotContain(deleted.getTicketId(), otherProject.getTicketId());
    }

    private TicketEntity sampleTicket(String title) {
        return new TicketEntity(title, "Description", TicketStatus.TODO, TicketPriority.MEDIUM, TicketType.BUG, projectId);
    }
}
