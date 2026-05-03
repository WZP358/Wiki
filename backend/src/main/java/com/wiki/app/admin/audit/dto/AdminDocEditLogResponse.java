package com.wiki.app.admin.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminDocEditLogResponse {
    private Long id;
    private Long docId;
    private Long userId;
    private String username;
    private String action;
    private String titleBefore;
    private String titleAfter;
    private Integer contentLengthBefore;
    private Integer contentLengthAfter;
    private String ip;
    private String commitMessage;
    private LocalDateTime createdAt;
}

