package com.wiki.app.admin;

import com.wiki.app.admin.dto.OperationLogResponse;
import com.wiki.app.log.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wiki.app.common.ApiResponse;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final OperationLogRepository logRepository;

    public AdminController(OperationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("/logs")
    public ApiResponse<Page<OperationLogResponse>> logs(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) String action,
                                                        @RequestParam(required = false) String targetType,
                                                        @RequestParam(required = false) String targetId,
                                                        @RequestParam(required = false) String ip,
                                                        @RequestParam(required = false) String fromTime,
                                                        @RequestParam(required = false) String toTime) {
        LocalDateTime from = parseTime(fromTime);
        LocalDateTime to = parseTime(toTime);
        Page<OperationLogResponse> result = logRepository.adminSearch(
                        userId,
                        action == null ? null : action.trim(),
                        targetType == null ? null : targetType.trim(),
                        targetId == null ? null : targetId.trim(),
                        ip == null ? null : ip.trim(),
                        from,
                        to,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(log -> OperationLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .username(log.getUsername())
                        .action(log.getAction())
                        .targetType(log.getTargetType())
                        .targetId(log.getTargetId())
                        .ip(log.getIp())
                        .detail(log.getDetail())
                        .createdAt(log.getCreatedAt())
                        .build());
        return ApiResponse.ok(result);
    }

    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            // supports ISO like 2026-03-17T12:34:56
            return LocalDateTime.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
