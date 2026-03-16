package com.wiki.app.doc.search;

import com.wiki.app.doc.WikiDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "wiki.search-es-enabled", havingValue = "false", matchIfMissing = true)
public class DocumentSearchService implements IDocumentSearchService {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void upsert(WikiDocument doc) {
        // No-op when Elasticsearch is disabled
    }

    @Override
    public void markDeleted(Long docId) {
        // No-op when Elasticsearch is disabled
    }

    @Override
    public void delete(Long docId) {
        // No-op when Elasticsearch is disabled
    }

    @Override
    public List<Long> searchDocIds(Long kbId, String keyword) {
        return null; // Fallback to MySQL LIKE search
    }

    @Override
    public void rebuildIndex(List<WikiDocument> docs) {
        // No-op when Elasticsearch is disabled
    }
}
