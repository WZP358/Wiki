package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DraftResponse {
    private Long docId;
    private Long userId;
    private String title;
    private String markdownContent;
    private LocalDateTime updatedAt;
}
