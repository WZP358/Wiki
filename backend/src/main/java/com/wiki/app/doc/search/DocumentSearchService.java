package com.wiki.app.doc.search;

import com.wiki.app.config.AppProperties;
import com.wiki.app.doc.WikiDocument;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DocumentSearchService {
    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);
    private static final int SEARCH_LIMIT = 200;

    private final AppProperties appProperties;
    private final WikiDocumentSearchRepository searchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public DocumentSearchService(AppProperties appProperties,
                                 WikiDocumentSearchRepository searchRepository,
                                 ElasticsearchOperations elasticsearchOperations) {
        this.appProperties = appProperties;
        this.searchRepository = searchRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public boolean isEnabled() {
        return appProperties.isSearchEsEnabled();
    }

    public void upsert(WikiDocument doc) {
        if (!isEnabled()) {
            return;
        }
        try {
            searchRepository.save(toSearchEntity(doc));
        } catch (Exception e) {
            log.warn("Elasticsearch upsert failed for doc {}", doc.getId(), e);
        }
    }

    public void markDeleted(Long docId) {
        if (!isEnabled()) {
            return;
        }
        try {
            searchRepository.findById(docId.toString()).ifPresent(entity -> {
                entity.setDeleted(true);
                searchRepository.save(entity);
            });
        } catch (Exception e) {
            log.warn("Elasticsearch markDeleted failed for doc {}", docId, e);
        }
    }

    public void delete(Long docId) {
        if (!isEnabled()) {
            return;
        }
        try {
            searchRepository.deleteById(docId.toString());
        } catch (Exception e) {
            log.warn("Elasticsearch delete failed for doc {}", docId, e);
        }
    }

    public List<Long> searchDocIds(Long kbId, String keyword) {
        if (!isEnabled()) {
            return null;
        }
        try {
            NativeQuery query = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("kbId").value(kbId)))
                            .must(m -> m.term(t -> t.field("deleted").value(false)))
                            .must(m -> m.bool(s -> s
                                    .should(c -> c.match(ma -> ma.field("title").query(keyword)))
                                    .should(c -> c.match(ma -> ma.field("markdownContent").query(keyword)))
                                    .minimumShouldMatch("1")
                            ))
                    ))
                    .withSort(sort -> sort.field(f -> f.field("updatedAt").order(SortOrder.Desc)))
                    .withMaxResults(SEARCH_LIMIT)
                    .build();

            SearchHits<WikiDocumentSearchEntity> hits = elasticsearchOperations.search(query, WikiDocumentSearchEntity.class);
            return hits.getSearchHits().stream()
                    .map(hit -> hit.getContent().getDocId())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.warn("Elasticsearch search failed for kb={}, fallback to LIKE", kbId, e);
            return null;
        }
    }

    public void rebuildIndex(List<WikiDocument> docs) {
        if (!isEnabled()) {
            return;
        }
        try {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            List<WikiDocumentSearchEntity> entities = docs.stream()
                    .map(this::toSearchEntity)
                    .toList();
            searchRepository.saveAll(entities);
        } catch (Exception e) {
            log.warn("Elasticsearch rebuild index failed", e);
        }
    }

    private WikiDocumentSearchEntity toSearchEntity(WikiDocument doc) {
        WikiDocumentSearchEntity entity = new WikiDocumentSearchEntity();
        entity.setId(doc.getId().toString());
        entity.setDocId(doc.getId());
        entity.setKbId(doc.getKbId());
        entity.setOwnerId(doc.getOwnerId());
        entity.setVisibility(doc.getVisibility() == null ? null : doc.getVisibility().name());
        entity.setPublished(Boolean.TRUE.equals(doc.getPublished()));
        entity.setDeleted(doc.getDeletedAt() != null);
        entity.setTitle(doc.getTitle());
        entity.setMarkdownContent(doc.getMarkdownContent());
        entity.setVersionNo(doc.getVersionNo());
        entity.setUpdatedAt(doc.getUpdatedAt());
        return entity;
    }
}
