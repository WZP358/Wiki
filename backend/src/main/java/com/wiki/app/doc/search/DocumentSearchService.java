package com.wiki.app.doc.search;

import com.wiki.app.doc.WikiDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentSearchService implements IDocumentSearchService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void upsert(WikiDocument doc) {
        // No-op: search uses MySQL LIKE through WikiDocumentRepository.
    }

    @Override
    public void markDeleted(Long docId) {
        // No-op: search uses MySQL LIKE through WikiDocumentRepository.
    }

    @Override
    public void delete(Long docId) {
        // No-op: search uses MySQL LIKE through WikiDocumentRepository.
    }

    @Override
    public List<Long> searchDocIds(Long kbId, String keyword) {
        return null;
    }

    @Override
    public void rebuildIndex(List<WikiDocument> docs) {
        // No-op: search uses MySQL LIKE through WikiDocumentRepository.
    }
}
