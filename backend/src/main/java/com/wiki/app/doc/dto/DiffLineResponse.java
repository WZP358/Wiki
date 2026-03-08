package com.wiki.app.doc.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiffLineResponse {
    private String type;
    private String left;
    private String right;
}
