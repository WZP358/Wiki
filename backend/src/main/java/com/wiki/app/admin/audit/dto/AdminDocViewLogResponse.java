package com.wiki.app.admin.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminDocViewLogResponse {
    private Long id;
    private Long docId;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}

