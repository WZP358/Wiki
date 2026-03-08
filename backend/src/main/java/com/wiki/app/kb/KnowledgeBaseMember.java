package com.wiki.app.kb;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "knowledge_base_members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_kb_user", columnNames = {"kb_id", "user_id"})
}, indexes = {
        @Index(name = "idx_kbm_kb", columnList = "kb_id"),
        @Index(name = "idx_kbm_user", columnList = "user_id")
})
public class KnowledgeBaseMember extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long kbId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberRole role;
}
