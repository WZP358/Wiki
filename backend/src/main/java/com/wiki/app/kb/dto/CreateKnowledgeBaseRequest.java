package com.wiki.app.kb.dto;

import com.wiki.app.kb.KnowledgeBaseType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateKnowledgeBaseRequest {
    @NotBlank
    private String name;

    @NotNull
    private KnowledgeBaseType type;

    private String description;
}
