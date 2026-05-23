package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.CommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByTicket_TicketId(Long ticketId);
}
