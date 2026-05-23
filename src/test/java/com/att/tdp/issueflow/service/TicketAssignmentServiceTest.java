package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
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
class TicketAssignmentServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private long projectId;
    private long devOlderId;
    private long devNewerId;

    @BeforeEach
    void setUp() {
        UserEntity owner = new UserEntity("assignowner", "assignowner@example.com", "Owner", Role.ADMIN);
        owner.setPassword("secret");
        owner = userRepository.save(owner);

        UserEntity olderDev = new UserEntity("devolder", "devolder@example.com", "Older Dev", Role.DEVELOPER);
        olderDev.setPassword("secret");
        devOlderId = userRepository.save(olderDev).getId();

        UserEntity newerDev = new UserEntity("devnewer", "devnewer@example.com", "Newer Dev", Role.DEVELOPER);
        newerDev.setPassword("secret");
        devNewerId = userRepository.save(newerDev).getId();

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("AssignProject-" + System.nanoTime());
        projectRequest.setDescription("Auto-assign tests");
        projectRequest.setOwnerId(owner.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    /** Goal: createTicket without assignee auto-assigns least-loaded developer and logs AUTO_ASSIGN. */
    @Test
    void createTicket_withoutAssignee_assignsLeastLoadedDeveloper() {
        assignOpenTicket(devNewerId);
        assignOpenTicket(devNewerId);

        var created = ticketService.createTicket(baseRequest());

        assertThat(created.getAssigneeId()).isEqualTo(devOlderId);
        assertThat(auditLogService.getAuditLogs(EntityType.TICKET, created.getTicketId(), AuditAction.AUTO_ASSIGN, ActorType.SYSTEM, null))
                .hasSize(1);
    }

    /** Goal: createTicket leaves assignee null when no developers exist. */
    @Test
    void createTicket_noDevelopers_leavesUnassigned() {
        userRepository.deleteById(devOlderId);
        userRepository.deleteById(devNewerId);

        var created = ticketService.createTicket(baseRequest());

        assertThat(created.getAssigneeId()).isNull();
    }

    /** Goal: createTicket with explicit assignee skips auto-assignment and AUTO_ASSIGN log. */
    @Test
    void createTicket_withExplicitAssignee_skipsAutoAssign() {
        var created = ticketService.createTicket(baseRequestWithAssignee(devNewerId));

        assertThat(created.getAssigneeId()).isEqualTo(devNewerId);
        assertThat(auditLogService.getAuditLogs(EntityType.TICKET, created.getTicketId(), AuditAction.AUTO_ASSIGN, null, null))
                .isEmpty();
    }

    private void assignOpenTicket(long assigneeId) {
        ticketService.createTicket(baseRequestWithAssignee(assigneeId));
    }

    private CreateTicketRequest baseRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Unassigned");
        request.setDescription("Needs assignee");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(TicketPriority.MEDIUM);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        return request;
    }

    private CreateTicketRequest baseRequestWithAssignee(long assigneeId) {
        CreateTicketRequest request = baseRequest();
        request.setAssigneeId(assigneeId);
        return request;
    }
}
