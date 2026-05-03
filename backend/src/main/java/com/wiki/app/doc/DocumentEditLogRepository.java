package com.wiki.app.doc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DocumentEditLogRepository extends JpaRepository<DocumentEditLog, Long> {
    Page<DocumentEditLog> findByDocIdOrderByCreatedAtDesc(Long docId, Pageable pageable);

    Page<DocumentEditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<DocumentEditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    @Query("select count(e) from DocumentEditLog e where e.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM DocumentEditLog e WHERE e.createdAt < :cutoffDate")
    long deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
