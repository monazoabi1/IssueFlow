package com.att.tdp.issueflow.stress;

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
import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.service.TicketEscalationService;
import com.att.tdp.issueflow.service.TicketService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("stress")
@SpringBootTest
@Transactional
class TicketEscalationStressTest {

    private static final int TICKET_COUNT = 80;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketEscalationService escalationService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("escstress", "escstress@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("EscStress-" + System.nanoTime());
        projectRequest.setDescription("Escalation stress");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    /** Goal: Bulk overdue tickets escalate to CRITICAL with overdue flag after runs. */
    @Test
    void processOverdueTickets_manyTickets_escalatesInSteps() {
        Instant pastDue = Instant.now().minusSeconds(3600);
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < TICKET_COUNT; i++) {
            CreateTicketRequest request = baseRequest("Ticket " + i, TicketPriority.LOW, pastDue);
            ids.add(ticketService.createTicket(request).getTicketId());
        }

        for (int run = 0; run < 4; run++) {
            escalationService.processOverdueTickets();
        }

        long criticalCount =
                ids.stream()
                        .map(id -> ticketRepository.findById(id).orElseThrow())
                        .filter(t -> t.getPriority() == TicketPriority.CRITICAL)
                        .count();

        long overdueFlagCount =
                ids.stream()
                        .map(id -> ticketRepository.findById(id).orElseThrow())
                        .filter(t -> t.isOverdue())
                        .count();

        assertThat(criticalCount).isEqualTo(TICKET_COUNT);
        assertThat(overdueFlagCount).isEqualTo(TICKET_COUNT);
    }

    private CreateTicketRequest baseRequest(String title, TicketPriority priority, Instant dueDate) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle(title);
        request.setDescription("Stress body");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(priority);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        request.setDueDate(dueDate);
        return request;
    }
}
