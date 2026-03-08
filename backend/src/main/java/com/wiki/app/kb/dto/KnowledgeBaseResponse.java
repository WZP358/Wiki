package com.wiki.app.kb.dto;

import com.wiki.app.kb.KnowledgeBaseType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeBaseResponse {
    private Long id;
    private String name;
    private KnowledgeBaseType type;
    private String description;
    private Long ownerId;
    private String myRole;
}
