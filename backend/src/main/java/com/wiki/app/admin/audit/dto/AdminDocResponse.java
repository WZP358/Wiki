package com.wiki.app.admin.audit.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminDocResponse {
    private Long id;
    private Long kbId;
    private Long parentId;
    private String title;
    private Long ownerId;
    private String visibility;
    private Boolean published;
    private Long viewCount;
    private Integer versionNo;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

