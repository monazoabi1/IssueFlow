package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.repository.TicketRepository;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketEscalationService {

    private static final Map<TicketPriority, TicketPriority> NEXT_PRIORITY = new EnumMap<>(TicketPriority.class);

    static {
        NEXT_PRIORITY.put(TicketPriority.LOW, TicketPriority.MEDIUM);
        NEXT_PRIORITY.put(TicketPriority.MEDIUM, TicketPriority.HIGH);
        NEXT_PRIORITY.put(TicketPriority.HIGH, TicketPriority.CRITICAL);
    }

    private final TicketRepository ticketRepository;

    public TicketEscalationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Scheduled(fixedDelayString = "${issueflow.escalation.interval-ms:60000}")
    @Transactional
    public void escalateOverdueTicketsScheduled() {
        processOverdueTickets();
    }

    @Transactional
    public void processOverdueTickets() {
        List<TicketEntity> candidates = ticketRepository.findEscalationCandidates(Instant.now());
        for (TicketEntity ticket : candidates) {
            escalateIfNeeded(ticket);
        }
    }

    void escalateIfNeeded(TicketEntity ticket) {
        if (ticket.getDueDate() == null || ticket.isDeleted() || !isPastDue(ticket)) {
            return;
        }

        if (ticket.getPriority() == TicketPriority.CRITICAL) {
            if (!ticket.isOverdue()) {
                ticket.setOverdue(true);
                ticketRepository.save(ticket);
            }
            return;
        }

        TicketPriority next = NEXT_PRIORITY.get(ticket.getPriority());
        if (next != null) {
            ticket.setPriority(next);
            ticketRepository.save(ticket);
        }
    }

    private boolean isPastDue(TicketEntity ticket) {
        return ticket.getDueDate().isBefore(Instant.now());
    }
}
