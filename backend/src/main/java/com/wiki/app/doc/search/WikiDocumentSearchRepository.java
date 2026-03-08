package com.wiki.app.doc.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface WikiDocumentSearchRepository extends ElasticsearchRepository<WikiDocumentSearchEntity, String> {
}
