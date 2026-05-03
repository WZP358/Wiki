package com.wiki.app.reaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentReactionRepository extends JpaRepository<DocumentReaction, Long> {
    Optional<DocumentReaction> findByDocumentIdAndUserIdAndReactionType(Long documentId, Long userId, ReactionType reactionType);

    List<DocumentReaction> findByDocumentIdAndDeletedAtIsNull(Long documentId);

    List<DocumentReaction> findByUserIdAndDeletedAtIsNull(Long userId);

    @Query("SELECT r.reactionType, COUNT(r) FROM DocumentReaction r " +
           "WHERE r.documentId = :docId AND r.deletedAt IS NULL " +
           "GROUP BY r.reactionType")
    List<Object[]> countByDocumentIdGroupByType(@Param("docId") Long docId);

    @Query("SELECT COUNT(r) FROM DocumentReaction r " +
           "WHERE r.documentId = :docId AND r.reactionType = :type AND r.deletedAt IS NULL")
    Long countByDocumentIdAndType(@Param("docId") Long docId, @Param("type") ReactionType type);

    boolean existsByDocumentIdAndUserIdAndReactionTypeAndDeletedAtIsNull(Long documentId, Long userId, ReactionType reactionType);
}
