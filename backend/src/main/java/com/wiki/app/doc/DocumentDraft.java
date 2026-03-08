package com.wiki.app.doc;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_drafts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_draft_doc_user", columnNames = {"doc_id", "user_id"})
})
public class DocumentDraft extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long docId;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 256)
    private String title;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String markdownContent;
}
