package com.att.tdp.issueflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ForbiddenException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.ProjectEntity;
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
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private TicketService ticketService;

    private long ownerId;
    private UserEntity adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new UserEntity("powner", "powner@example.com", "Project Owner", Role.ADMIN);
        adminUser.setPassword("secret");
        adminUser = userRepository.save(adminUser);
        ownerId = adminUser.getId();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** Goal: createProject persists project and writes CREATE audit log. */
    @Test
    void createProject_persistsProjectWithOwner() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("IssueFlow");
        request.setDescription("Ticket platform");
        request.setOwnerId(ownerId);

        var created = projectService.createProject(request);

        assertThat(created.getId()).isPositive();
        assertThat(created.getName()).isEqualTo("IssueFlow");
        assertThat(created.getOwner().getId()).isEqualTo(ownerId);
        assertThat(created.isDeleted()).isFalse();
        assertThat(auditLogService.getAuditLogs(EntityType.PROJECT, created.getId(), AuditAction.CREATE, null, null))
                .hasSize(1)
                .first()
                .satisfies(log -> {
                    assertThat(log.getAction()).isEqualTo(AuditAction.CREATE);
                    assertThat(log.getEntityType()).isEqualTo(EntityType.PROJECT);
                    assertThat(log.getActor()).isEqualTo(ActorType.USER);
                });
    }

    /** Goal: createProject with unknown owner throws ResourceNotFoundException. */
    @Test
    void createProject_unknownOwner_throwsNotFound() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("Orphan");
        request.setDescription("No owner");
        request.setOwnerId(99999L);

        assertThatThrownBy(() -> projectService.createProject(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found");
        assertThat(auditLogService.getAuditLogs(EntityType.PROJECT, null, AuditAction.CREATE, null, null))
                .isEmpty();
    }

    /** Goal: updateProject changes fields and records UPDATE audit log. */
    @Test
    void updateProject_updatesNameAndDescription() {
        long projectId = createSampleProject().getId();

        UpdateProjectRequest update = new UpdateProjectRequest();
        update.setName("IssueFlow v2");
        update.setDescription("Updated description");

        var updated = projectService.updateProject(projectId, update);

        assertThat(updated.getName()).isEqualTo("IssueFlow v2");
        assertThat(updated.getDescription()).isEqualTo("Updated description");
        assertThat(auditLogService.getAuditLogs(EntityType.PROJECT, projectId, AuditAction.UPDATE, null, null))
                .hasSize(1)
                .first()
                .satisfies(log -> assertThat(log.getAction()).isEqualTo(AuditAction.UPDATE));
    }

    /** Goal: deleteProject soft-deletes and records DELETE audit log. */
    @Test
    void deleteProject_softDeletesProject() {
        long projectId = createSampleProject().getId();

        projectService.deleteProject(projectId);

        assertThat(projectService.getProjectById(projectId)).isEmpty();
        assertThat(auditLogService.getAuditLogs(EntityType.PROJECT, projectId, AuditAction.DELETE, null, null))
                .hasSize(1)
                .first()
                .satisfies(log -> assertThat(log.getAction()).isEqualTo(AuditAction.DELETE));
    }

    /** Goal: deleteProject on already deleted project throws ConflictException. */
    @Test
    void deleteProject_alreadyDeleted_throwsConflict() {
        long projectId = createSampleProject().getId();
        projectService.deleteProject(projectId);

        assertThatThrownBy(() -> projectService.deleteProject(projectId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Project already deleted");
    }

    /** Goal: getAllProjects omits soft-deleted projects. */
    @Test
    void getAllProjects_excludesSoftDeleted() {
        long activeId = createSampleProject().getId();
        long deletedId = createSampleProject("Deleted Project").getId();
        projectService.deleteProject(deletedId);

        assertThat(projectService.getAllProjects())
                .extracting(p -> p.getId())
                .contains(activeId)
                .doesNotContain(deletedId);
    }

    /** Goal: getDeletedProjects as non-admin throws ForbiddenException. */
    @Test
    void getDeletedProjects_requiresAdmin() {
        UserEntity developer = new UserEntity("pdev", "pdev@example.com", "Dev", Role.DEVELOPER);
        developer.setPassword("secret");
        userRepository.save(developer);
        authenticateAs(developer);

        assertThatThrownBy(() -> projectService.getDeletedProjects())
                .isInstanceOf(ForbiddenException.class);
    }

    /** Goal: getProjectWorkload orders developers by ascending open ticket count. */
    @Test
    void getProjectWorkload_sortedByOpenTicketCount() {
        UserEntity dev1 = new UserEntity("wload1", "wload1@example.com", "Dev One", Role.DEVELOPER);
        dev1.setPassword("secret");
        dev1 = userRepository.save(dev1);
        UserEntity dev2 = new UserEntity("wload2", "wload2@example.com", "Dev Two", Role.DEVELOPER);
        dev2.setPassword("secret");
        dev2 = userRepository.save(dev2);

        long projectId = createSampleProject("Workload Project").getId();
        createAssignedTicket(projectId, dev2.getId());
        createAssignedTicket(projectId, dev2.getId());

        var workload = projectService.getProjectWorkload(projectId);

        assertThat(workload).hasSizeGreaterThanOrEqualTo(2);
        assertThat(workload.get(0).getUserId()).isEqualTo(dev1.getId());
        assertThat(workload.get(0).getOpenTicketCount()).isZero();
        assertThat(workload.get(workload.size() - 1).getOpenTicketCount()).isGreaterThanOrEqualTo(2);
    }

    /** Goal: restoreProject removes project from deleted list and makes it fetchable. */
    @Test
    void restoreProject_makesProjectVisibleAgain() {
        long projectId = createSampleProject("Restorable").getId();
        projectService.deleteProject(projectId);
        authenticateAs(adminUser);

        assertThat(projectService.getDeletedProjects())
                .extracting(ProjectEntity::getId)
                .contains(projectId);

        projectService.restoreProject(projectId);

        assertThat(projectService.getProjectById(projectId)).isPresent();
        assertThat(projectService.getDeletedProjects())
                .extracting(ProjectEntity::getId)
                .doesNotContain(projectId);
    }

    private void authenticateAs(UserEntity user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private ProjectEntity createSampleProject() {
        return createSampleProject("Sample Project");
    }

    private ProjectEntity createSampleProject(String name) {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName(name);
        request.setDescription("Description for " + name);
        request.setOwnerId(ownerId);
        return projectService.createProject(request);
    }

    private void createAssignedTicket(long projectId, long assigneeId) {
        CreateTicketRequest request = new CreateTicketRequest();
        request.setTitle("Workload ticket");
        request.setDescription("Body");
        request.setStatus(TicketStatus.TODO);
        request.setPriority(TicketPriority.MEDIUM);
        request.setType(TicketType.BUG);
        request.setProjectId(projectId);
        request.setAssigneeId(assigneeId);
        ticketService.createTicket(request);
    }
}
