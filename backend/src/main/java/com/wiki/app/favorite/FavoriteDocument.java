package com.wiki.app.favorite;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "favorite_documents", indexes = {
        @Index(name = "idx_favorite_user_kb", columnList = "user_id,kb_id"),
        @Index(name = "idx_favorite_user_doc", columnList = "user_id,doc_id", unique = true)
})
public class FavoriteDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;
}
