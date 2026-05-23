package com.att.tdp.issueflow.stress;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import com.att.tdp.issueflow.service.ProjectService;
import com.att.tdp.issueflow.service.TicketService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@Tag("stress")
@SpringBootTest
@Transactional
class TicketAssignmentStressTest {

    private static final int DEVELOPER_COUNT = 8;
    private static final int TICKET_COUNT = 64;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    private long projectId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("assignstress", "assignstress@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("AssignStress-" + System.nanoTime());
        projectRequest.setDescription("Assignment stress");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();

        for (int i = 0; i < DEVELOPER_COUNT; i++) {
            UserEntity dev = new UserEntity("devstress" + i, "devstress" + i + "@example.com", "Dev " + i, Role.DEVELOPER);
            dev.setPassword("secret");
            userRepository.save(dev);
        }
    }

    /** Goal: Many unassigned tickets auto-assign evenly across developers (max-min ≤ 1). */
    @Test
    void createManyUnassignedTickets_balancesAcrossDevelopers() {
        Map<Long, Integer> counts = new HashMap<>();

        for (int i = 0; i < TICKET_COUNT; i++) {
            CreateTicketRequest request = new CreateTicketRequest();
            request.setTitle("Stress ticket " + i);
            request.setDescription("Body");
            request.setStatus(TicketStatus.TODO);
            request.setPriority(TicketPriority.MEDIUM);
            request.setType(TicketType.BUG);
            request.setProjectId(projectId);

            Long assigneeId = ticketService.createTicket(request).getAssigneeId();
            assertThat(assigneeId).isNotNull();
            counts.merge(assigneeId, 1, Integer::sum);
        }

        int min = counts.values().stream().mapToInt(v -> v).min().orElse(0);
        int max = counts.values().stream().mapToInt(v -> v).max().orElse(0);

        assertThat(counts).hasSize(DEVELOPER_COUNT);
        assertThat(max - min).isLessThanOrEqualTo(1);
    }
}
