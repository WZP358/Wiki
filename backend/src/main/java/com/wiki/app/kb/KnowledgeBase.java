package com.wiki.app.kb;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "knowledge_bases", indexes = {
        @Index(name = "idx_kb_owner", columnList = "owner_id")
})
public class KnowledgeBase extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KnowledgeBaseType type;

    @Column(nullable = false)
    private Long ownerId;

    @Column(length = 512)
    private String description;
}
