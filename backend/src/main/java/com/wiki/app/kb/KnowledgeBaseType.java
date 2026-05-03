package com.wiki.app.kb;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum KnowledgeBaseType {
    COMPANY,
    DEPARTMENT,
    PRIVATE;

    @JsonCreator
    public static KnowledgeBaseType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase();
        if ("PUBLIC".equals(value)) {
            return COMPANY;
        }
        return KnowledgeBaseType.valueOf(value);
    }
}
