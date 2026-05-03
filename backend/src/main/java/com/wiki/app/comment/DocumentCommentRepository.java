package com.wiki.app.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentCommentRepository extends JpaRepository<DocumentComment, Long> {
    List<DocumentComment> findByDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long documentId);

    List<DocumentComment> findByDocumentIdAndParentIdIsNullAndDeletedAtIsNullOrderByCreatedAtDesc(Long documentId);

    List<DocumentComment> findByParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long parentId);

    @Query("SELECT COUNT(c) FROM DocumentComment c WHERE c.documentId = :docId AND c.deletedAt IS NULL")
    Long countByDocumentId(@Param("docId") Long docId);

    List<DocumentComment> findByAuthorIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long authorId);
}
