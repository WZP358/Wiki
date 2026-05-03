package com.wiki.app.kb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeBaseTypeTest {

    @Test
    void mapsLegacyPublicToCompany() {
        assertEquals(KnowledgeBaseType.COMPANY, KnowledgeBaseType.from("PUBLIC"));
        assertEquals(KnowledgeBaseType.COMPANY, KnowledgeBaseType.from("public"));
    }

    @Test
    void mapsBlankToNullForValidationLayer() {
        assertNull(KnowledgeBaseType.from(null));
        assertNull(KnowledgeBaseType.from(" "));
    }

    @Test
    void converterNormalizesLegacyDatabaseValue() {
        KnowledgeBaseTypeConverter converter = new KnowledgeBaseTypeConverter();
        assertEquals(KnowledgeBaseType.COMPANY, converter.convertToEntityAttribute("PUBLIC"));
        assertEquals("COMPANY", converter.convertToDatabaseColumn(KnowledgeBaseType.COMPANY));
    }
}
