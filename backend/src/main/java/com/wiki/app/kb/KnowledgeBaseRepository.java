package com.wiki.app.kb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByOwnerIdAndDeletedAtIsNull(Long ownerId);

    List<KnowledgeBase> findByTypeAndDeletedAtIsNull(KnowledgeBaseType type);

    List<KnowledgeBase> findByOwnerIdAndTypeAndDeletedAtIsNull(Long ownerId, KnowledgeBaseType type);

    List<KnowledgeBase> findByParentIdAndDeletedAtIsNull(Long parentId);

    List<KnowledgeBase> findByTeamIdAndDeletedAtIsNull(Long teamId);

    @Query("SELECT kb FROM KnowledgeBase kb " +
            "WHERE kb.deletedAt IS NULL " +
            "AND (LOWER(kb.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(COALESCE(kb.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
            select kb from KnowledgeBase kb
            where (:keyword is null or :keyword = ''
                or lower(kb.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(kb.description, '')) like lower(concat('%', :keyword, '%'))
            )
            and (:type is null or kb.type = :type)
            and (:ownerId is null or kb.ownerId = :ownerId)
            and (:deleted is null
                 or (:deleted = true and kb.deletedAt is not null)
                 or (:deleted = false and kb.deletedAt is null)
            )
            """)
    Page<KnowledgeBase> adminSearch(@Param("keyword") String keyword,
                                   @Param("type") KnowledgeBaseType type,
                                   @Param("ownerId") Long ownerId,
                                   @Param("deleted") Boolean deleted,
                                   Pageable pageable);
}
