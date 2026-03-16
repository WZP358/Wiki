package com.wiki.app.doc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "document_edit_logs", indexes = {
        @Index(name = "idx_edit_doc", columnList = "doc_id"),
        @Index(name = "idx_edit_user", columnList = "user_id"),
        @Index(name = "idx_edit_time", columnList = "created_at"),
        @Index(name = "idx_edit_action", columnList = "action")
})
public class DocumentEditLog {
    @Id
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(nullable = false, length = 32)
    private String action;

    @Column(name = "title_before", length = 256)
    private String titleBefore;

    @Column(name = "title_after", length = 256)
    private String titleAfter;

    @Column(name = "content_length_before")
    private Integer contentLengthBefore;

    @Column(name = "content_length_after")
    private Integer contentLengthAfter;

    @Column(length = 64)
    private String ip;

    @Column(name = "commit_message", length = 256)
    private String commitMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
