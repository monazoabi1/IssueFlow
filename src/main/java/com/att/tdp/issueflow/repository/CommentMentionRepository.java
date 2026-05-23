package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.model.CommentEntity;
import com.att.tdp.issueflow.model.CommentMentionEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, Long> {

    List<CommentMentionEntity> findByComment_CommentId(Long commentId);

    void deleteByComment_CommentId(Long commentId);

    @Query(
            """
            SELECT m.comment FROM CommentMentionEntity m
            WHERE m.user.id = :userId
            ORDER BY m.comment.commentId DESC
            """)
    Page<CommentEntity> findCommentsByMentionedUserId(@Param("userId") Long userId, Pageable pageable);
}
