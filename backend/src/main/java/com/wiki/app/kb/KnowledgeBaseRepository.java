package com.wiki.app.kb;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    List<KnowledgeBase> findByTypeAndDeletedAtIsNull(KnowledgeBaseType type);
}
