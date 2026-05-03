package com.wiki.app.kb;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseMemberRepository extends JpaRepository<KnowledgeBaseMember, Long> {
    List<KnowledgeBaseMember> findByKbIdAndDeletedAtIsNull(Long kbId);

    List<KnowledgeBaseMember> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<KnowledgeBaseMember> findByKbIdAndUserIdAndDeletedAtIsNull(Long kbId, Long userId);

    Optional<KnowledgeBaseMember> findByKbIdAndUserId(Long kbId, Long userId);

    long countByKbIdAndRoleAndDeletedAtIsNull(Long kbId, MemberRole role);
}
