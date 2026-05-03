package com.wiki.app.kb;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class KnowledgeBaseTypeConverter implements AttributeConverter<KnowledgeBaseType, String> {

    @Override
    public String convertToDatabaseColumn(KnowledgeBaseType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public KnowledgeBaseType convertToEntityAttribute(String dbData) {
        return KnowledgeBaseType.from(dbData);
    }
}
