package com.wiki.app.doc.dto;

import com.wiki.app.doc.DocVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDocumentRequest {
    @NotNull
    private Long kbId;

    private Long parentId;

    @NotBlank
    private String title;

    @NotBlank
    private String markdownContent;

    @NotNull
    private DocVisibility visibility;

    private Boolean published = true;
}
