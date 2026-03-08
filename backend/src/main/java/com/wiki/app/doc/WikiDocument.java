package com.wiki.app.doc;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_doc_kb", columnList = "kb_id"),
        @Index(name = "idx_doc_parent", columnList = "parent_id"),
        @Index(name = "idx_doc_deleted", columnList = "deleted_at")
})
public class WikiDocument extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long kbId;

    private Long parentId;

    @Column(nullable = false, length = 256)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String markdownContent;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Column(nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DocVisibility visibility;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Integer versionNo = 1;

    @Column(nullable = false)
    private Boolean published = true;
}
