package com.wiki.app.kb;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeBaseMemberSyncInitializer implements ApplicationRunner {
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public KnowledgeBaseMemberSyncInitializer(KnowledgeBaseMemberSyncService memberSyncService) {
        this.memberSyncService = memberSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        memberSyncService.syncAllAutoReaders();
    }
}
