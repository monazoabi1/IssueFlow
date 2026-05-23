package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.TicketEntity.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, Long> {

    List<TicketEntity> findAllByProjectIdAndIsDeletedFalse(Long projectId);

    List<TicketEntity> findAllByProjectIdAndIsDeletedTrue(Long projectId);

    Optional<TicketEntity> findByTicketIdAndIsDeletedFalse(Long ticketId);

    Optional<TicketEntity> findByTicketIdAndIsDeletedTrue(Long ticketId);

    @Query(
            """
            SELECT t FROM TicketEntity t
            WHERE t.dueDate IS NOT NULL
              AND t.dueDate < :now
              AND t.isDeleted = false
              AND t.status <> com.att.tdp.issueflow.model.TicketEntity.TicketStatus.DONE
            """)
    List<TicketEntity> findEscalationCandidates(@Param("now") Instant now);

    long countByProjectIdAndAssigneeIdAndStatusNotAndIsDeletedFalse(
            Long projectId, Long assigneeId, TicketStatus status);
}
