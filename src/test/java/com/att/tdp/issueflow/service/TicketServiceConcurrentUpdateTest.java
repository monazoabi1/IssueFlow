package com.att.tdp.issueflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceConcurrentUpdateTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AuthService authService;

    @Mock
    private TicketEscalationService ticketEscalationService;

    @Mock
    private TicketAssignmentService ticketAssignmentService;

    @Mock
    private TicketDependencyService ticketDependencyService;

    @InjectMocks
    private TicketService ticketService;

    /** Goal: updateTicket with stale version throws ConflictException with current version hint. */
    @Test
    void updateTicket_staleVersion_throwsConflict() {
        TicketEntity ticket = new TicketEntity(
                "Bug", "desc", TicketStatus.TODO, TicketPriority.HIGH, TicketType.BUG, 1L);
        ticket.setTicketId(10L);
        ReflectionTestUtils.setField(ticket, "version", 2L);

        when(ticketRepository.findByTicketIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(ticket));

        UpdateTicketRequest request = new UpdateTicketRequest();
        request.setVersion(1L);
        request.setTitle("Updated");

        ConflictException ex = assertThrows(
                ConflictException.class, () -> ticketService.updateTicket(10L, request));
        assertEquals(
                "Ticket was modified by another user. Refresh and retry with version 2",
                ex.getMessage());
    }
}
