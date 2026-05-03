package com.wiki.app.doc;

import org.springframework.stereotype.Component;

@Component
public class RecycleCleanupScheduler {
    public void purgeExpiredRecycleDocs() {
        // Deleted documents are retained for administrator review and restore.
    }
}
