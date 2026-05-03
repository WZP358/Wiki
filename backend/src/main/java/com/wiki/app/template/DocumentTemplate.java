package com.wiki.app.template;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_templates", indexes = {
        @Index(name = "idx_template_kb", columnList = "kb_id"),
        @Index(name = "idx_template_creator", columnList = "creator_id")
})
public class DocumentTemplate extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "markdown_content", columnDefinition = "LONGTEXT")
    private String markdownContent;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "use_count", nullable = false)
    private Integer useCount = 0;

    @Column(length = 64)
    private String category;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;
}
