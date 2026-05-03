package com.wiki.app.admin.stats.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AdminOverviewStatsResponse {
    private long activeUsers;
    private long totalDocs;
    private long deletedDocs;
    private long operationLogs24h;
    private long docViews24h;
    private long docEdits24h;

    private List<DailyCount> operationLogs7d;

    @Data
    @Builder
    public static class DailyCount {
        private LocalDate date;
        private long count;
    }
}

