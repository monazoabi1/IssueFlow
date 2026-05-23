package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.model.UserEntity.Role;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class TicketAssignmentService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public TicketAssignmentService(
            UserRepository userRepository,
            TicketRepository ticketRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.ticketRepository = ticketRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Assigns the least-loaded DEVELOPER (by open tickets in the project) when no assignee was provided.
     *
     * @return true if an assignee was set automatically
     */
    public boolean autoAssignIfNeeded(TicketEntity ticket) {
        if (ticket.getAssigneeId() != null) {
            return false;
        }
        Optional<UserEntity> assignee = findLeastLoadedDeveloper(ticket.getProjectId());
        if (assignee.isEmpty()) {
            return false;
        }
        ticket.setAssigneeId(assignee.get().getId());
        return true;
    }

    public void logAutoAssignment(long ticketId) {
        auditLogService.log(AuditAction.AUTO_ASSIGN, EntityType.TICKET, ticketId, null, ActorType.SYSTEM);
    }

    public Optional<UserEntity> findLeastLoadedDeveloper(long projectId) {
        List<UserEntity> developers = userRepository.findByRoleOrderByIdAsc(Role.DEVELOPER);
        if (developers.isEmpty()) {
            return Optional.empty();
        }
        return developers.stream()
                .min(Comparator.<UserEntity>comparingLong(dev -> openTicketCount(projectId, dev.getId()))
                        .thenComparingLong(UserEntity::getId));
    }

    private long openTicketCount(long projectId, long userId) {
        return ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndIsDeletedFalse(
                projectId, userId, TicketStatus.DONE);
    }
}
