package com.wiki.app.share;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "share_links", indexes = {
        @Index(name = "idx_share_token", columnList = "token", unique = true),
        @Index(name = "idx_share_doc", columnList = "doc_id")
})
public class ShareLink extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(nullable = false)
    private Long docId;

    @Column(nullable = false)
    private Long creatorId;

    private LocalDateTime expireAt;
}
