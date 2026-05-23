package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.AttachmentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<AttachmentEntity, Long> {

    Optional<AttachmentEntity> findByIdAndTicketId(Long id, Long ticketId);
}
