package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentVersionResponse {
    private Long id;
    private Integer versionNo;
    private String title;
    private String markdownContent;
    private String htmlContent;
    private Long editorId;
    private String editorName;
    private String commitMessage;
    private LocalDateTime createdAt;
}
