package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentTreeNode {
    private Long id;
    private Long parentId;
    private String title;
    private Integer versionNo;
}
