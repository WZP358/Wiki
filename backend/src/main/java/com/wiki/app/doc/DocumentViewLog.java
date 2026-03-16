package com.wiki.app.doc;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "document_view_logs", indexes = {
        @Index(name = "idx_view_doc", columnList = "doc_id"),
        @Index(name = "idx_view_user", columnList = "user_id"),
        @Index(name = "idx_view_time", columnList = "created_at")
})
public class DocumentViewLog {
    @Id
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "view_duration")
    private Integer viewDuration;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
