package com.wiki.app.template.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateResponse {
    private String id;
    private String name;
    private String description;
    private String kbId;
    private String creatorId;
    private String creatorName;
    private String markdownContent;
    private Boolean isPublic;
    private Integer useCount;
    private String category;
    private String coverUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
