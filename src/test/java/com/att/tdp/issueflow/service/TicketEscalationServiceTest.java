package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketEscalationServiceTest {

    @Autowired
    private TicketEscalationService escalationService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("escowner", "escowner@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("EscalationProject-" + System.nanoTime());
        projectRequest.setDescription("Escalation tests");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    /** Goal: Repeated escalation promotes LOW overdue ticket to CRITICAL with overdue flag. */
    @Test
    void processOverdueTickets_fullChainFromLowToCriticalAndOverdue() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.LOW, Instant.now().minusSeconds(60)));

        for (int i = 0; i < 4; i++) {
            escalationService.processOverdueTickets();
        }

        var updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(updated.isOverdue()).isTrue();
    }

    /** Goal: Single escalation pass promotes overdue LOW ticket one priority level. */
    @Test
    void processOverdueTickets_promotesPriorityOneLevel() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.LOW, Instant.now().minusSeconds(60)));
        escalationService.processOverdueTickets();

        var updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(updated.isOverdue()).isFalse();
    }

    /** Goal: Overdue CRITICAL ticket gets isOverdue set without priority change. */
    @Test
    void processOverdueTickets_criticalSetsOverdueFlag() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.CRITICAL, Instant.now().minusSeconds(60)));
        escalationService.processOverdueTickets();

        var updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(updated.isOverdue()).isTrue();
    }

    /** Goal: Running escalation twice on overdue CRITICAL ticket is idempotent. */
    @Test
    void processOverdueTickets_criticalIdempotent() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.CRITICAL, Instant.now().minusSeconds(60)));
        escalationService.processOverdueTickets();
        escalationService.processOverdueTickets();

        var updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(updated.isOverdue()).isTrue();
    }

    /** Goal: Tickets without due date are not escalated. */
    @Test
    void processOverdueTickets_skipsTicketsWithoutDueDate() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.LOW, null));
        escalationService.processOverdueTickets();

        var updated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.LOW);
    }

    /** Goal: Manual priority update clears the overdue flag on escalated ticket. */
    @Test
    void manualPriorityChange_clearsOverdueFlag() {
        var ticket = ticketService.createTicket(ticketRequest(TicketPriority.CRITICAL, Instant.now().minusSeconds(60)));
        escalationService.processOverdueTickets();
        ticketRepository.flush();
        var escalated = ticketRepository.findById(ticket.getTicketId()).orElseThrow();
        assertThat(escalated.isOverdue()).isTrue();

        var update = new com.att.tdp.issueflow.dto.UpdateTicketRequest();
        update.setVersion(escalated.getVersion());
        update.setPriority(TicketPriority.HIGH);
        ticketService.updateTicket(ticket.getTicketId(), update);

        assertThat(ticketRepository.findById(ticket.getTicketId()).orElseThrow().isOverdue()).isFalse();
    }

    private CreateTicketRequest ticketRequest(TicketPriority priority, Instant dueDate) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Escalation ticket");
        request.setDescription("Body");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(priority);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        request.setDueDate(dueDate);
        return request;
    }
}
