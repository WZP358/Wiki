package com.wiki.app.doc;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncCleanupService {
    private final LocalDocStorageService localDocStorageService;
    private final CloudStorageCleanupService cloudStorageCleanupService;

    public AsyncCleanupService(LocalDocStorageService localDocStorageService,
                               CloudStorageCleanupService cloudStorageCleanupService) {
        this.localDocStorageService = localDocStorageService;
        this.cloudStorageCleanupService = cloudStorageCleanupService;
    }

    @Async
    public void cleanupDocumentArtifacts(Long docId) {
        localDocStorageService.deleteDocArtifacts(docId);
        cloudStorageCleanupService.cleanupDoc(docId);
    }
}
