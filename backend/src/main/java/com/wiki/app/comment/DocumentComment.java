package com.wiki.app.comment;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_comments", indexes = {
        @Index(name = "idx_comment_doc", columnList = "document_id"),
        @Index(name = "idx_comment_author", columnList = "author_id"),
        @Index(name = "idx_comment_parent", columnList = "parent_id")
})
public class DocumentComment extends BaseEntity {
    @Id
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "is_resolved", nullable = false)
    private Boolean isResolved = false;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private java.time.LocalDateTime resolvedAt;
}
