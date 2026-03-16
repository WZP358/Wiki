package com.wiki.app.favorite;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteDocumentRepository extends JpaRepository<FavoriteDocument, Long> {

    @Query("SELECT f FROM FavoriteDocument f WHERE f.userId = :userId AND f.kbId = :kbId ORDER BY f.createdAt DESC")
    List<FavoriteDocument> findByUserIdAndKbId(Long userId, Long kbId);

    @Query("SELECT f FROM FavoriteDocument f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<FavoriteDocument> findByUserId(Long userId);

    Optional<FavoriteDocument> findByUserIdAndDocId(Long userId, Long docId);

    boolean existsByUserIdAndDocId(Long userId, Long docId);

    void deleteByUserIdAndDocId(Long userId, Long docId);
}
