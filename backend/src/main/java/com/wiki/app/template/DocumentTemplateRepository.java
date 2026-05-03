package com.wiki.app.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    List<DocumentTemplate> findByKbIdAndDeletedAtIsNull(Long kbId);

    List<DocumentTemplate> findByIsPublicTrueAndDeletedAtIsNull();

    List<DocumentTemplate> findByCreatorIdAndDeletedAtIsNull(Long creatorId);

    @Query("SELECT t FROM DocumentTemplate t WHERE t.deletedAt IS NULL " +
           "AND (t.isPublic = true OR t.kbId = :kbId OR t.creatorId = :userId) " +
           "ORDER BY t.useCount DESC, t.createdAt DESC")
    List<DocumentTemplate> findAvailableTemplates(@Param("kbId") Long kbId, @Param("userId") Long userId);

    @Query("SELECT t FROM DocumentTemplate t WHERE t.deletedAt IS NULL " +
           "AND t.category = :category " +
           "AND (t.isPublic = true OR t.kbId = :kbId OR t.creatorId = :userId)")
    List<DocumentTemplate> findByCategory(@Param("category") String category,
                                          @Param("kbId") Long kbId,
                                          @Param("userId") Long userId);

    @Query("""
            select t from DocumentTemplate t
            where (:keyword is null or :keyword = ''
                or lower(t.name) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(t.description, '')) like lower(concat('%', :keyword, '%'))
            )
            and (:kbId is null or t.kbId = :kbId)
            and (:deleted is null
                 or (:deleted = true and t.deletedAt is not null)
                 or (:deleted = false and t.deletedAt is null)
            )
            """)
    Page<DocumentTemplate> adminSearch(@Param("keyword") String keyword,
                                       @Param("kbId") Long kbId,
                                       @Param("deleted") Boolean deleted,
                                       Pageable pageable);
}
