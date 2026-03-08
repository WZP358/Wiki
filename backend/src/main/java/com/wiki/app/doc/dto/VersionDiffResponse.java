package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VersionDiffResponse {
    private Long docId;
    private Long leftVersionId;
    private Long rightVersionId;
    private Integer leftVersionNo;
    private Integer rightVersionNo;
    private List<DiffLineResponse> lines;
}
