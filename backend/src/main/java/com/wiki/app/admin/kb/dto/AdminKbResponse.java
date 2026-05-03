package com.wiki.app.admin.kb.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminKbResponse {
    private Long id;
    private String name;
    private String type;
    private Long ownerId;
    private Long teamId;
    private String teamName;
    private String description;
    private boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

