package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.CommentResponse;
import com.att.tdp.issueflow.dto.CreateCommentRequest;
import com.att.tdp.issueflow.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.model.CommentEntity;
import com.att.tdp.issueflow.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public List<CommentResponse> getComments(@PathVariable Long ticketId) {
        return commentService.getCommentsForTicket(ticketId).stream()
                .map(comment -> new CommentResponse(
                        comment, commentService.getMentionedUsers(comment.getCommentId())))
                .collect(Collectors.toList());
    }

    @PostMapping
    public CommentResponse createComment(
            @PathVariable Long ticketId,
            @Valid @RequestBody CreateCommentRequest request) {
        CommentEntity comment = commentService.addComment(ticketId, request);
        return new CommentResponse(comment, commentService.getMentionedUsers(comment.getCommentId()));
    }

    @PatchMapping("/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentEntity comment = commentService.updateComment(ticketId, commentId, request);
        return new CommentResponse(comment, commentService.getMentionedUsers(comment.getCommentId()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId) {
        commentService.deleteComment(ticketId, commentId);
        return ResponseEntity.ok().build();
    }
}
