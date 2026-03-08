package com.wiki.app.doc.dto;

import com.wiki.app.doc.DocVisibility;
import lombok.Data;

@Data
public class UpdateDocumentRequest {
    private String title;
    private String markdownContent;
    private DocVisibility visibility;
    private Integer baseVersion;
    private String commitMessage;
    private Boolean published;
}
