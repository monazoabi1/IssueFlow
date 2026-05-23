package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.Exception.ConflictException;
import com.att.tdp.issueflow.Exception.ResourceNotFoundException;
import com.att.tdp.issueflow.dto.CreateCommentRequest;
import com.att.tdp.issueflow.dto.MentionedUserResponse;
import com.att.tdp.issueflow.dto.MentionsPageResponse;
import com.att.tdp.issueflow.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.model.AuditLogEntity.ActorType;
import com.att.tdp.issueflow.model.AuditLogEntity.AuditAction;
import com.att.tdp.issueflow.model.AuditLogEntity.EntityType;
import com.att.tdp.issueflow.model.CommentEntity;
import com.att.tdp.issueflow.model.CommentMentionEntity;
import com.att.tdp.issueflow.model.TicketEntity;
import com.att.tdp.issueflow.model.UserEntity;
import com.att.tdp.issueflow.repository.CommentMentionRepository;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]+)");
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CommentRepository commentRepository;
    private final CommentMentionRepository commentMentionRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public CommentService(
            CommentRepository commentRepository,
            CommentMentionRepository commentMentionRepository,
            TicketRepository ticketRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.commentRepository = commentRepository;
        this.commentMentionRepository = commentMentionRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public List<CommentEntity> getCommentsForTicket(Long ticketId) {
        ticketRepository.findByTicketIdAndIsDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        return commentRepository.findByTicket_TicketId(ticketId);
    }

    @Transactional
    public CommentEntity addComment(Long ticketId, CreateCommentRequest request) {
        TicketEntity ticket = ticketRepository.findByTicketIdAndIsDeletedFalse(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        UserEntity author = userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateMentions(request.getContent());

        CommentEntity comment = new CommentEntity(request.getContent(), ticket, author);
        CommentEntity saved = commentRepository.save(comment);
        syncMentions(saved, request.getContent());
        auditLogService.log(
                AuditAction.CREATE,
                EntityType.COMMENT,
                saved.getCommentId(),
                request.getAuthorId(),
                ActorType.USER);
        return saved;
    }

    @Transactional
    public CommentEntity updateComment(Long ticketId, Long commentId, UpdateCommentRequest request) {
        CommentEntity comment = getCommentForTicket(ticketId, commentId);
        validateMentions(request.getContent());
        comment.setContent(request.getContent());
        CommentEntity saved = commentRepository.save(comment);
        syncMentions(saved, request.getContent());
        auditLogService.log(
                AuditAction.UPDATE,
                EntityType.COMMENT,
                saved.getCommentId(),
                saved.getAuthor().getId(),
                ActorType.USER);
        return saved;
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId) {
        CommentEntity comment = getCommentForTicket(ticketId, commentId);
        long commentEntityId = comment.getCommentId();
        Long authorId = comment.getAuthor().getId();
        commentMentionRepository.deleteByComment_CommentId(commentEntityId);
        commentRepository.delete(comment);
        auditLogService.log(
                AuditAction.DELETE, EntityType.COMMENT, commentEntityId, authorId, ActorType.USER);
    }

    public List<MentionedUserResponse> getMentionedUsers(long commentId) {
        return commentMentionRepository.findByComment_CommentId(commentId).stream()
                .map(mention -> new MentionedUserResponse(mention.getUser()))
                .collect(Collectors.toList());
    }

    public MentionsPageResponse getMentionsForUser(Long userId, Integer page, Integer pageSize) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int resolvedPage = page == null || page < 1 ? 1 : page;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;

        Page<CommentEntity> result = commentMentionRepository.findCommentsByMentionedUserId(
                userId, PageRequest.of(resolvedPage - 1, resolvedPageSize));

        List<com.att.tdp.issueflow.dto.CommentResponse> comments = result.getContent().stream()
                .map(comment -> new com.att.tdp.issueflow.dto.CommentResponse(
                        comment, getMentionedUsers(comment.getCommentId())))
                .collect(Collectors.toList());

        return new MentionsPageResponse(comments, result.getTotalElements(), resolvedPage);
    }

    private void syncMentions(CommentEntity comment, String content) {
        List<UserEntity> targetUsers = resolveMentionedUsers(content);
        Set<Long> targetIds =
                targetUsers.stream().map(UserEntity::getId).collect(Collectors.toSet());

        List<CommentMentionEntity> existing =
                commentMentionRepository.findByComment_CommentId(comment.getCommentId());

        for (CommentMentionEntity mention : existing) {
            if (!targetIds.contains(mention.getUser().getId())) {
                commentMentionRepository.delete(mention);
            }
        }

        Set<Long> existingIds =
                existing.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());

        for (UserEntity user : targetUsers) {
            if (!existingIds.contains(user.getId())) {
                commentMentionRepository.save(new CommentMentionEntity(comment, user));
            }
        }
    }

    private List<UserEntity> resolveMentionedUsers(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }
        List<UserEntity> mentioned = new ArrayList<>();
        for (String username : usernames) {
            userRepository
                    .findByUsernameIgnoreCase(username)
                    .ifPresent(mentioned::add);
        }
        return mentioned;
    }

    private void validateMentions(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            String username = matcher.group(1);
            if (userRepository.findByUsernameIgnoreCase(username).isEmpty()) {
                throw new ConflictException("Unknown mentioned user: @" + username);
            }
        }
    }

    private CommentEntity getCommentForTicket(Long ticketId, Long commentId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
        if (comment.getTicket().getTicketId() != ticketId) {
            throw new ResourceNotFoundException("Comment not found for this ticket");
        }
        return comment;
    }
}
