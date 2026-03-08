package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RealtimeDocSnapshot {
    private Long docId;
    private String title;
    private String markdownContent;
    private Integer versionNo;
    private Long ownerId;
}
