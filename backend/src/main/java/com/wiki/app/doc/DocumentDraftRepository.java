package com.wiki.app.doc;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentDraftRepository extends JpaRepository<DocumentDraft, Long> {
    Optional<DocumentDraft> findByDocIdAndUserId(Long docId, Long userId);

    void deleteByDocId(Long docId);
}
