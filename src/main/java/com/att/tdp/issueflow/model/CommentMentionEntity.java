package com.att.tdp.issueflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "comment_mentions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}))
public class CommentMentionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    protected CommentMentionEntity() {}

    public CommentMentionEntity(CommentEntity comment, UserEntity user) {
        this.comment = comment;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public CommentEntity getComment() {
        return comment;
    }

    public UserEntity getUser() {
        return user;
    }
}
