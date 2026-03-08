package com.wiki.app.doc.dto;

import lombok.Data;

@Data
public class SaveDraftRequest {
    private String title;
    private String markdownContent;
}
