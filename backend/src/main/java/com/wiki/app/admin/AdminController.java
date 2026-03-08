package com.wiki.app.admin;

import com.wiki.app.admin.dto.OperationLogResponse;
import com.wiki.app.log.OperationLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wiki.app.common.ApiResponse;

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
                                                        @RequestParam(defaultValue = "20") int size) {
        Page<OperationLogResponse> result = logRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
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
}
