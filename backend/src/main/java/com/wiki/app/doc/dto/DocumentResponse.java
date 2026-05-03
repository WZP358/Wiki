package com.wiki.app.doc.dto;

import com.wiki.app.doc.DocVisibility;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private Long kbId;
    private Long parentId;
    private String title;
    private String markdownContent;
    private String htmlContent;
    private Long ownerId;
    private String ownerUsername;
    private String ownerName;
    private DocVisibility visibility;
    private Long viewCount;
    private Integer versionNo;
    private Boolean published;
    private LocalDateTime updatedAt;
    private String searchHighlight;
}
