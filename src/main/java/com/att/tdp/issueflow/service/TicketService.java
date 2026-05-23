package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateTicketRequest;
import com.att.tdp.issueflow.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Map<TicketStatus, TicketStatus> NEXT_STATUS = Map.of(
            TicketStatus.TODO, TicketStatus.IN_PROGRESS,
            TicketStatus.IN_PROGRESS, TicketStatus.IN_REVIEW,
            TicketStatus.IN_REVIEW, TicketStatus.DONE);

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AuthService authService;
    private final TicketEscalationService ticketEscalationService;
    private final TicketAssignmentService ticketAssignmentService;
    private final TicketDependencyService ticketDependencyService;

    public TicketService(
            TicketRepository ticketRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            AuthService authService,
            TicketEscalationService ticketEscalationService,
            TicketAssignmentService ticketAssignmentService,
            TicketDependencyService ticketDependencyService) {
        this.ticketRepository = ticketRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.authService = authService;
        this.ticketEscalationService = ticketEscalationService;
        this.ticketAssignmentService = ticketAssignmentService;
        this.ticketDependencyService = ticketDependencyService;
    }

    public Optional<TicketEntity> getTicketById(Long id) {
        ticketEscalationService.processOverdueTickets();
        return ticketRepository.findByTicketIdAndIsDeletedFalse(id);
    }

    public List<TicketEntity> getAllTicketsByProjectId(Long projectId) {
        ticketEscalationService.processOverdueTickets();
        return ticketRepository.findAllByProjectIdAndIsDeletedFalse(projectId);
    }

    @Transactional
    public TicketEntity createTicket(CreateTicketRequest request) {
        projectRepository.findByIdAndIsDeletedFalse(request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (request.getAssigneeId() != null) {
            validateAssigneeExists(request.getAssigneeId());
        }

        TicketEntity ticket = request.toEntity();
        boolean autoAssigned = ticketAssignmentService.autoAssignIfNeeded(ticket);
        TicketEntity saved = ticketRepository.save(ticket);
        auditLogService.log(AuditAction.CREATE, EntityType.TICKET, saved.getTicketId());
        if (autoAssigned) {
            ticketAssignmentService.logAutoAssignment(saved.getTicketId());
        }
        return saved;
    }

    @Transactional
    public TicketEntity updateTicket(Long id, UpdateTicketRequest request) {
        TicketEntity existingTicket = ticketRepository.findByTicketIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (existingTicket.isDeleted()) {
            throw new ConflictException("Ticket already deleted");
        }

        assertVersionMatches(existingTicket, request.getVersion());

        if (request.getAssigneeId() != null) {
            validateAssigneeExists(request.getAssigneeId());
        }

        if (request.getPriority() != null) {
            existingTicket.setOverdue(false);
        }

        if (request.getStatus() != null) {
            validateStatusChange(existingTicket, request.getStatus());
            existingTicket.setStatus(request.getStatus());
        }

        request.applyTo(existingTicket);
        TicketEntity saved = ticketRepository.saveAndFlush(existingTicket);
        auditLogService.log(AuditAction.UPDATE, EntityType.TICKET, saved.getTicketId());
        return saved;
    }

    @Transactional
    public void deleteTicket(Long id) {
        TicketEntity existingTicket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        if (existingTicket.isDeleted()) {
            throw new ConflictException("Ticket already deleted");
        }
        existingTicket.setDeleted(true);
        ticketRepository.save(existingTicket);
        auditLogService.log(AuditAction.DELETE, EntityType.TICKET, id);
    }

    public List<TicketEntity> getDeletedTicketsByProjectId(Long projectId) {
        authService.requireAdmin();
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        return ticketRepository.findAllByProjectIdAndIsDeletedTrue(projectId);
    }

    @Transactional
    public TicketEntity restoreTicket(Long id) {
        authService.requireAdmin();
        TicketEntity ticket = ticketRepository.findByTicketIdAndIsDeletedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deleted ticket not found"));
        ticket.setDeleted(false);
        TicketEntity saved = ticketRepository.save(ticket);
        auditLogService.log(AuditAction.UPDATE, EntityType.TICKET, saved.getTicketId());
        return saved;
    }

    void validateStatusChange(TicketEntity ticket, TicketStatus requested) {
        TicketStatus current = ticket.getStatus();
        if (requested == null || current == requested) {
            return;
        }
        if (current == TicketStatus.DONE) {
            throw new ConflictException("Cannot change status: ticket is already DONE");
        }
        if (requested == TicketStatus.DONE) {
            ticketDependencyService.validateCanTransitionToDone(ticket);
        }
        TicketStatus allowedNext = NEXT_STATUS.get(current);
        if (requested != allowedNext) {
            throw new ConflictException(
                    "Invalid status transition: " + current + " → " + requested
                            + ". Allowed: " + current + " → " + allowedNext);
        }
    }

    private void assertVersionMatches(TicketEntity ticket, Long clientVersion) {
        if (clientVersion == null) {
            throw new ConflictException("Ticket version is required for update");
        }
        if (clientVersion != ticket.getVersion()) {
            throw new ConflictException(
                    "Ticket was modified by another user. Refresh and retry with version "
                            + ticket.getVersion());
        }
    }

    private void validateAssigneeExists(Long assigneeId) {
        userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
