package com.wiki.app.doc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文档日志清理定时任务
 * 自动清理超过15天的查看记录和修改记录，避免数据膨胀
 */
@Component
public class DocumentLogCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(DocumentLogCleanupScheduler.class);
    private static final int RETENTION_DAYS = 15;

    private final DocumentViewLogRepository viewLogRepository;
    private final DocumentEditLogRepository editLogRepository;

    public DocumentLogCleanupScheduler(DocumentViewLogRepository viewLogRepository,
                                       DocumentEditLogRepository editLogRepository) {
        this.viewLogRepository = viewLogRepository;
        this.editLogRepository = editLogRepository;
    }

    /**
     * 每天凌晨3点执行日志清理任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);

        try {
            // 清理查看记录
            long viewLogsDeleted = viewLogRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Cleaned up {} view logs older than {} days", viewLogsDeleted, RETENTION_DAYS);

            // 清理修改记录
            long editLogsDeleted = editLogRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Cleaned up {} edit logs older than {} days", editLogsDeleted, RETENTION_DAYS);

            log.info("Document log cleanup completed successfully. Total deleted: {} records",
                    viewLogsDeleted + editLogsDeleted);
        } catch (Exception e) {
            log.error("Failed to cleanup old document logs", e);
        }
    }
}
