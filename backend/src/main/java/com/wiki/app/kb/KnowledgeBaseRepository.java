package com.wiki.app.kb;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    List<KnowledgeBase> findByTypeAndDeletedAtIsNull(KnowledgeBaseType type);

    @Query("SELECT kb FROM KnowledgeBase kb WHERE kb.ownerId = :userId AND kb.type = 'COMPANY' AND kb.deletedAt IS NULL")
    List<KnowledgeBase> findPublicByUserId(@Param("userId") Long userId);

    @Query("SELECT kb FROM KnowledgeBase kb " +
            "WHERE kb.deletedAt IS NULL " +
            "AND (LOWER(kb.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(kb.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword);
}
