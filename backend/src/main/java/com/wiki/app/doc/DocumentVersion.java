package com.wiki.app.doc;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_versions", indexes = {
        @Index(name = "idx_dv_doc", columnList = "doc_id")
})
public class DocumentVersion extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long docId;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 256)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String markdownContent;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Column(nullable = false)
    private Long editorId;

    @Column(length = 64)
    private String editorName;

    @Column(length = 256)
    private String commitMessage;
}
