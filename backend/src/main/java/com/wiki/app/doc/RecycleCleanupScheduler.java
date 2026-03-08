package com.wiki.app.doc;

import com.wiki.app.config.AppProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RecycleCleanupScheduler {
    private final WikiDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentDraftRepository draftRepository;
    private final AsyncCleanupService asyncCleanupService;
    private final AppProperties appProperties;

    public RecycleCleanupScheduler(WikiDocumentRepository documentRepository,
                                   DocumentVersionRepository versionRepository,
                                   DocumentDraftRepository draftRepository,
                                   AsyncCleanupService asyncCleanupService,
                                   AppProperties appProperties) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.draftRepository = draftRepository;
        this.asyncCleanupService = asyncCleanupService;
        this.appProperties = appProperties;
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void purgeExpiredRecycleDocs() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(appProperties.getRecycleReserveDays());
        List<WikiDocument> expired = documentRepository.findByDeletedAtBefore(deadline);
        for (WikiDocument doc : expired) {
            versionRepository.deleteByDocId(doc.getId());
            draftRepository.deleteByDocId(doc.getId());
            documentRepository.delete(doc);
            asyncCleanupService.cleanupDocumentArtifacts(doc.getId());
        }
    }
}
