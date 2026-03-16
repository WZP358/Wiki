package com.wiki.app.doc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface DocumentViewLogRepository extends JpaRepository<DocumentViewLog, Long> {
    Page<DocumentViewLog> findByDocIdOrderByCreatedAtDesc(Long docId, Pageable pageable);

    Page<DocumentViewLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT v.userId) FROM DocumentViewLog v WHERE v.docId = :docId")
    Long countDistinctViewersByDocId(Long docId);

    @Query("SELECT COUNT(v) FROM DocumentViewLog v WHERE v.docId = :docId AND v.createdAt >= :since")
    Long countViewsSince(Long docId, LocalDateTime since);

    @Modifying
    @Query("DELETE FROM DocumentViewLog v WHERE v.createdAt < :cutoffDate")
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
