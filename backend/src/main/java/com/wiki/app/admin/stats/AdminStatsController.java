package com.wiki.app.admin.stats;

import com.wiki.app.admin.stats.dto.AdminOverviewStatsResponse;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.doc.DocumentEditLogRepository;
import com.wiki.app.doc.DocumentViewLogRepository;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.log.OperationLogRepository;
import com.wiki.app.user.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final UserRepository userRepository;
    private final WikiDocumentRepository documentRepository;
    private final OperationLogRepository operationLogRepository;
    private final DocumentViewLogRepository viewLogRepository;
    private final DocumentEditLogRepository editLogRepository;

    public AdminStatsController(UserRepository userRepository,
                                WikiDocumentRepository documentRepository,
                                OperationLogRepository operationLogRepository,
                                DocumentViewLogRepository viewLogRepository,
                                DocumentEditLogRepository editLogRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.operationLogRepository = operationLogRepository;
        this.viewLogRepository = viewLogRepository;
        this.editLogRepository = editLogRepository;
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverviewStatsResponse> overview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since24h = now.minusHours(24);

        long activeUsers = userRepository.countByDeletedAtIsNull();
        long totalDocs = documentRepository.countByDeletedAtIsNull();
        long deletedDocs = documentRepository.countByDeletedAtIsNotNull();
        long op24h = operationLogRepository.countSince(since24h);
        long views24h = viewLogRepository.countSince(since24h);
        long edits24h = editLogRepository.countSince(since24h);

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(6);
        LocalDateTime fromTime = from.atStartOfDay();
        LocalDateTime toTime = to.plusDays(1).atStartOfDay().minusNanos(1);

        Map<LocalDate, Long> series = new HashMap<>();
        for (Object[] row : operationLogRepository.dailyCounts(fromTime, toTime)) {
            // row[0] may be java.sql.Date or LocalDate depending on JPA provider
            LocalDate d = row[0] instanceof java.sql.Date sd ? sd.toLocalDate() : (LocalDate) row[0];
            long c = ((Number) row[1]).longValue();
            series.put(d, c);
        }
        List<AdminOverviewStatsResponse.DailyCount> op7d = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            op7d.add(AdminOverviewStatsResponse.DailyCount.builder()
                    .date(d)
                    .count(series.getOrDefault(d, 0L))
                    .build());
        }

        return ApiResponse.ok(AdminOverviewStatsResponse.builder()
                .activeUsers(activeUsers)
                .totalDocs(totalDocs)
                .deletedDocs(deletedDocs)
                .operationLogs24h(op24h)
                .docViews24h(views24h)
                .docEdits24h(edits24h)
                .operationLogs7d(op7d)
                .build());
    }
}

