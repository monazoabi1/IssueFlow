package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.BadRequestException;
import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.AddDependencyRequest;
import com.att.tdp.issueflow.model.TicketDependencyEntity;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketDependencyService {

    private final TicketRepository ticketRepository;
    private final TicketDependencyRepository dependencyRepository;

    public TicketDependencyService(
            TicketRepository ticketRepository, TicketDependencyRepository dependencyRepository) {
        this.ticketRepository = ticketRepository;
        this.dependencyRepository = dependencyRepository;
    }

    @Transactional
    public void addDependency(Long ticketId, AddDependencyRequest request) {
        TicketEntity ticket = requireActiveTicket(ticketId);
        TicketEntity blocker = requireActiveTicket(request.getBlockedBy());

        if (ticket.getTicketId() == blocker.getTicketId()) {
            throw new BadRequestException("A ticket cannot block itself");
        }
        if (ticket.getProjectId() != blocker.getProjectId()) {
            throw new BadRequestException("Both tickets must belong to the same project");
        }
        if (dependencyRepository.existsByTicket_TicketIdAndBlockedByTicket_TicketId(
                ticketId, request.getBlockedBy())) {
            throw new ConflictException("Dependency already exists");
        }
        if (wouldCreateCycle(ticketId, request.getBlockedBy())) {
            throw new ConflictException("Adding this dependency would create a circular dependency");
        }

        dependencyRepository.save(new TicketDependencyEntity(ticket, blocker));
    }

    @Transactional(readOnly = true)
    public List<TicketEntity> listBlockers(Long ticketId) {
        requireActiveTicket(ticketId);
        return dependencyRepository.findByTicket_TicketId(ticketId).stream()
                .map(TicketDependencyEntity::getBlockedByTicket)
                .toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId) {
        requireActiveTicket(ticketId);
        TicketDependencyEntity dependency = dependencyRepository
                .findByTicket_TicketIdAndBlockedByTicket_TicketId(ticketId, blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependency not found"));
        dependencyRepository.delete(dependency);
    }

    public void validateCanTransitionToDone(TicketEntity ticket) {
        List<TicketDependencyEntity> dependencies = dependencyRepository.findByTicket_TicketId(ticket.getTicketId());
        for (TicketDependencyEntity dependency : dependencies) {
            TicketEntity blocker = dependency.getBlockedByTicket();
            if (blocker.getStatus() != TicketStatus.DONE) {
                throw new ConflictException(
                        "Cannot mark ticket as DONE: blocked by ticket " + blocker.getTicketId());
            }
        }
    }

    private boolean wouldCreateCycle(long ticketId, long blockedById) {
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(blockedById);

        while (!queue.isEmpty()) {
            long current = queue.poll();
            if (current == ticketId) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            for (TicketDependencyEntity dep : dependencyRepository.findByTicket_TicketId(current)) {
                queue.add(dep.getBlockedByTicket().getTicketId());
            }
        }
        return false;
    }

    private TicketEntity requireActiveTicket(Long ticketId) {
        return ticketRepository
                .findByTicketIdAndIsDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
    }
}
