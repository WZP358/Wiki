package com.wiki.app.reaction;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "document_reactions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "user_id", "reaction_type"}),
       indexes = {
        @Index(name = "idx_reaction_doc", columnList = "document_id"),
        @Index(name = "idx_reaction_user", columnList = "user_id")
})
public class DocumentReaction extends BaseEntity {
    @Id
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 32)
    private ReactionType reactionType;
}
