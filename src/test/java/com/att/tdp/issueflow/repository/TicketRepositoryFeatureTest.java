package com.att.tdp.issueflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketPriority;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import com.att.tdp.issueflow.model.TicketEntity.TicketType;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class TicketRepositoryFeatureTest {

    @Autowired
    private TicketRepository ticketRepository;

    /** Goal: findEscalationCandidates returns only overdue non-DONE tickets. */
    @Test
    void findEscalationCandidates_returnsOnlyOverdueActiveTickets() {
        TicketEntity overdue = sampleTicket("Overdue");
        overdue.setDueDate(Instant.now().minusSeconds(60));
        overdue = ticketRepository.save(overdue);

        TicketEntity future = sampleTicket("Future");
        future.setDueDate(Instant.now().plusSeconds(3600));
        ticketRepository.save(future);

        TicketEntity done = sampleTicket("Done");
        done.setDueDate(Instant.now().minusSeconds(60));
        done.setStatus(TicketStatus.DONE);
        ticketRepository.save(done);

        assertThat(ticketRepository.findEscalationCandidates(Instant.now()))
                .extracting(TicketEntity::getTicketId)
                .contains(overdue.getTicketId())
                .doesNotContain(future.getTicketId(), done.getTicketId());
    }

    /** Goal: Open ticket count query excludes DONE status tickets. */
    @Test
    void countOpenTicketsByAssignee_excludesDoneAndDeleted() {
        TicketEntity open = sampleTicket("Open");
        open.setAssigneeId(42L);
        open.setProjectId(7L);
        ticketRepository.save(open);

        TicketEntity done = sampleTicket("Done assignee");
        done.setAssigneeId(42L);
        done.setProjectId(7L);
        done.setStatus(TicketStatus.DONE);
        ticketRepository.save(done);

        long count = ticketRepository.countByProjectIdAndAssigneeIdAndStatusNotAndIsDeletedFalse(
                7L, 42L, TicketStatus.DONE);

        assertThat(count).isEqualTo(1);
    }

    private TicketEntity sampleTicket(String title) {
        TicketEntity ticket = new TicketEntity(title, "Body", TicketStatus.TODO, TicketPriority.MEDIUM, TicketType.BUG, 7L);
        ticket.setProjectId(7L);
        return ticket;
    }
}
