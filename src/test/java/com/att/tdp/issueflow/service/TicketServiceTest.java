package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ForbiddenException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    private long projectId;
    private long assigneeId;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity("ticketowner", "ticketowner@example.com", "Owner", Role.ADMIN);
        adminUser.setPassword("secret");
        adminUser = userRepository.save(adminUser);
        assigneeId = adminUser.getId();

        CreateProjectRequest projectRequest = new CreateProjectRequest();
        projectRequest.setName("TicketServiceProject-" + System.nanoTime());
        projectRequest.setDescription("For ticket service tests");
        projectRequest.setOwnerId(adminUser.getId());
        projectId = projectService.createProject(projectRequest).getId();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Goal: createTicket with unknown project throws ResourceNotFoundException. */
    @Test
    void createTicket_unknownProject_throwsNotFound() {
        CreateTicketRequest request = validCreateRequest();
        request.setProjectId(99999L);

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
    }

    /** Goal: createTicket on soft-deleted project throws ResourceNotFoundException. */
    @Test
    void createTicket_deletedProject_throwsNotFound() {
        projectService.deleteProject(projectId);

        assertThatThrownBy(() -> ticketService.createTicket(validCreateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found");
    }

    /** Goal: createTicket with unknown assignee throws ResourceNotFoundException. */
    @Test
    void createTicket_unknownAssignee_throwsNotFound() {
        CreateTicketRequest request = validCreateRequest();
        request.setAssigneeId(99999L);

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
    }

    /** Goal: createTicket without assignee succeeds and writes CREATE audit log. */
    @Test
    void createTicket_withoutAssignee_succeeds() {
        CreateTicketRequest request = validCreateRequest();
        request.setAssigneeId(null);

        TicketEntity created = ticketService.createTicket(request);

        assertThat(created.getTicketId()).isPositive();
        assertThat(created.getAssigneeId()).isNull();
        assertThat(auditLogService.getAuditLogs(
                        EntityType.TICKET, created.getTicketId(), AuditAction.CREATE, null, null))
                .hasSize(1)
                .first()
                .satisfies(log -> assertThat(log.getEntityType()).isEqualTo(EntityType.TICKET));
    }

    /** Goal: updateTicket with invalid status transition throws ConflictException. */
    @Test
    void updateTicket_invalidStatusTransition_throwsConflict() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(0L);
        update.setStatus(TicketStatus.DONE);

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Invalid status transition");
    }

    /** Goal: updateTicket changing status from DONE throws ConflictException. */
    @Test
    void updateTicket_fromDone_throwsConflict() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();
        advanceToDone(ticketId);

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(currentVersion(ticketId));
        update.setStatus(TicketStatus.IN_REVIEW);

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, update))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot change status: ticket is already DONE");
    }

    /** Goal: updateTicket without version throws ConflictException. */
    @Test
    void updateTicket_missingVersion_throwsConflict() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setTitle("No version");

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, update))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ticket version is required for update");
    }

    /** Goal: updateTicket with stale version throws ConflictException. */
    @Test
    void updateTicket_staleVersion_throwsConflict() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(999L);
        update.setTitle("Stale");

        assertThatThrownBy(() -> ticketService.updateTicket(ticketId, update))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ticket was modified by another user");
    }

    /** Goal: updateTicket with valid TODO to IN_PROGRESS transition succeeds. */
    @Test
    void updateTicket_validStatusTransition_succeeds() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();

        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(0L);
        update.setStatus(TicketStatus.IN_PROGRESS);

        TicketEntity updated = ticketService.updateTicket(ticketId, update);

        assertThat(updated.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    /** Goal: deleteTicket soft-deletes so getTicketById returns empty. */
    @Test
    void deleteTicket_thenNotVisibleViaGet() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();

        ticketService.deleteTicket(ticketId);

        assertThat(ticketService.getTicketById(ticketId)).isEmpty();
    }

    /** Goal: deleteTicket twice throws ConflictException on second call. */
    @Test
    void deleteTicket_twice_throwsConflict() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();
        ticketService.deleteTicket(ticketId);

        assertThatThrownBy(() -> ticketService.deleteTicket(ticketId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ticket already deleted");
    }

    /** Goal: getDeletedTicketsByProjectId as non-admin throws ForbiddenException. */
    @Test
    void getDeletedTickets_requiresAdmin() {
        UserEntity developer = new UserEntity("ticketdev", "ticketdev@example.com", "Dev", Role.DEVELOPER);
        developer.setPassword("secret");
        userRepository.save(developer);
        authenticateAs(developer);

        assertThatThrownBy(() -> ticketService.getDeletedTicketsByProjectId(projectId))
                .isInstanceOf(ForbiddenException.class);
    }

    /** Goal: restoreTicket removes ticket from deleted list and makes it fetchable. */
    @Test
    void restoreTicket_makesTicketVisibleAgain() {
        long ticketId = ticketService.createTicket(validCreateRequest()).getTicketId();
        ticketService.deleteTicket(ticketId);
        authenticateAs(adminUser);

        assertThat(ticketService.getDeletedTicketsByProjectId(projectId))
                .extracting(TicketEntity::getTicketId)
                .contains(ticketId);

        ticketService.restoreTicket(ticketId);

        assertThat(ticketService.getTicketById(ticketId)).isPresent();
        assertThat(ticketService.getDeletedTicketsByProjectId(projectId))
                .extracting(TicketEntity::getTicketId)
                .doesNotContain(ticketId);
    }

    private void authenticateAs(UserEntity user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private CreateTicketRequest validCreateRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Login bug");
        request.setDescription("Users cannot log in");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(TicketPriority.HIGH);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        request.setAssigneeId(assigneeId);
        return request;
    }

    private void advanceToDone(long ticketId) {
        updateStatus(ticketId, TicketStatus.IN_PROGRESS);
        updateStatus(ticketId, TicketStatus.IN_REVIEW);
        updateStatus(ticketId, TicketStatus.DONE);
    }

    private void updateStatus(long ticketId, TicketStatus status) {
        UpdateTicketRequest update = new UpdateTicketRequest();
        update.setVersion(currentVersion(ticketId));
        update.setStatus(status);
        ticketService.updateTicket(ticketId, update);
    }

    private long currentVersion(long ticketId) {
        return ticketService.getTicketById(ticketId).orElseThrow().getVersion();
    }
}
