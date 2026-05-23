package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.TicketDependencyEntity;
import com.att.tdp.issueflow.model.TicketEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketDependencyRepository extends JpaRepository<TicketDependencyEntity, Long> {

    List<TicketDependencyEntity> findByTicket_TicketId(Long ticketId);

    Optional<TicketDependencyEntity> findByTicket_TicketIdAndBlockedByTicket_TicketId(
            Long ticketId, Long blockedByTicketId);

    boolean existsByTicket_TicketIdAndBlockedByTicket_TicketId(Long ticketId, Long blockedByTicketId);

    List<TicketDependencyEntity> findByBlockedByTicket(TicketEntity blockedByTicket);
}
