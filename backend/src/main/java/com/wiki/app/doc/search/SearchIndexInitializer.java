package com.wiki.app.doc.search;

import com.wiki.app.config.AppProperties;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchIndexInitializer {
    private static final Logger log = LoggerFactory.getLogger(SearchIndexInitializer.class);

    private final AppProperties appProperties;
    private final WikiDocumentRepository documentRepository;
    private final DocumentSearchService documentSearchService;

    public SearchIndexInitializer(AppProperties appProperties,
                                  WikiDocumentRepository documentRepository,
                                  DocumentSearchService documentSearchService) {
        this.appProperties = appProperties;
        this.documentRepository = documentRepository;
        this.documentSearchService = documentSearchService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reindexIfConfigured() {
        if (!appProperties.isSearchEsEnabled() || !appProperties.isSearchEsReindexOnStartup()) {
            return;
        }
        List<WikiDocument> docs = documentRepository.findByDeletedAtIsNull();
        documentSearchService.rebuildIndex(docs);
        log.info("Elasticsearch reindex completed: {} documents", docs.size());
    }
}
