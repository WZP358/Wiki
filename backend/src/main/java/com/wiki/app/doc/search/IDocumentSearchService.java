package com.wiki.app.doc.search;

import com.wiki.app.doc.WikiDocument;

import java.util.List;

public interface IDocumentSearchService {
    boolean isEnabled();
    void upsert(WikiDocument doc);
    void markDeleted(Long docId);
    void delete(Long docId);
    List<Long> searchDocIds(Long kbId, String keyword);
    void rebuildIndex(List<WikiDocument> docs);
}
