package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateProjectRequest;
import com.att.tdp.issueflow.dto.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.WorkloadEntryResponse;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.ProjectEntity;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;
    private final AuthService authService;

    public ProjectService(
            ProjectRepository projectRepository,
            UserRepository userRepository,
            TicketRepository ticketRepository,
            AuditLogService auditLogService,
            AuthService authService) {
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
        this.authService = authService;
    }

    public List<ProjectEntity> getAllProjects() {
        return projectRepository.findAllByIsDeletedFalse();
    }

    public Optional<ProjectEntity> getProjectById(Long id) {
        return projectRepository.findByIdAndIsDeletedFalse(id);
    }

    public ProjectEntity createProject(CreateProjectRequest request) {
        UserEntity owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        ProjectEntity saved = projectRepository.save(request.toEntity(owner));
        auditLogService.log(AuditAction.CREATE, EntityType.PROJECT, saved.getId());
        return saved;
    }

    public ProjectEntity updateProject(Long id, UpdateProjectRequest request) {
        ProjectEntity existingProject = getProjectById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        request.applyTo(existingProject);
        ProjectEntity saved = projectRepository.save(existingProject);
        auditLogService.log(AuditAction.UPDATE, EntityType.PROJECT, saved.getId());
        return saved;
    }

    public void deleteProject(Long id) {
        ProjectEntity existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (existingProject.isDeleted()) {
            throw new ConflictException("Project already deleted");
        }
        existingProject.setDeleted(true);
        projectRepository.save(existingProject);
        auditLogService.log(AuditAction.DELETE, EntityType.PROJECT, id);
    }

    public List<ProjectEntity> getDeletedProjects() {
        authService.requireAdmin();
        return projectRepository.findAllByIsDeletedTrue();
    }

    public ProjectEntity restoreProject(Long id) {
        authService.requireAdmin();
        ProjectEntity project = projectRepository.findByIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted project not found"));
        project.setDeleted(false);
        ProjectEntity saved = projectRepository.save(project);
        auditLogService.log(AuditAction.UPDATE, EntityType.PROJECT, saved.getId());
        return saved;
    }

    public List<WorkloadEntryResponse> getProjectWorkload(Long projectId) {
        getProjectById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        return userRepository.findByRoleOrderByIdAsc(Role.DEVELOPER).stream()
                .map(dev -> new WorkloadEntryResponse(
                        dev.getId(),
                        dev.getUsername(),
                        ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndIsDeletedFalse(
                                projectId, dev.getId(), TicketStatus.DONE)))
                .sorted(Comparator.comparingLong(WorkloadEntryResponse::getOpenTicketCount)
                        .thenComparingLong(WorkloadEntryResponse::getUserId))
                .toList();
    }
}
