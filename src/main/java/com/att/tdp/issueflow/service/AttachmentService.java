package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.model.AttachmentEntity;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import com.att.tdp.issueflow.validation.AttachmentContentValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final AttachmentContentValidator attachmentContentValidator;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            TicketService ticketService,
            AttachmentContentValidator attachmentContentValidator) {
        this.attachmentRepository = attachmentRepository;
        this.ticketService = ticketService;
        this.attachmentContentValidator = attachmentContentValidator;
    }

    @Transactional
    public AttachmentEntity uploadAttachment(Long ticketId, MultipartFile file) {
        attachmentContentValidator.validate(file);

        ticketService
                .getTicketById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "attachment";
        }

        byte[] data = attachmentContentValidator.readContent(file);
        AttachmentEntity attachment = new AttachmentEntity(
                ticketId, filename, file.getContentType(), data.length, data);
        return attachmentRepository.save(attachment);
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId) {
        ticketService
                .getTicketById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        AttachmentEntity attachment = attachmentRepository
                .findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        attachmentRepository.delete(attachment);
    }
}
