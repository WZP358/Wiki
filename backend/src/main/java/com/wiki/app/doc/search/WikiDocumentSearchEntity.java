package com.wiki.app.doc.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Getter
@Setter
@Document(indexName = "wiki_documents")
public class WikiDocumentSearchEntity {
    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long docId;

    @Field(type = FieldType.Long)
    private Long kbId;

    @Field(type = FieldType.Long)
    private Long ownerId;

    @Field(type = FieldType.Keyword)
    private String visibility;

    @Field(type = FieldType.Boolean)
    private Boolean published;

    @Field(type = FieldType.Boolean)
    private Boolean deleted;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String markdownContent;

    @Field(type = FieldType.Integer)
    private Integer versionNo;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_fraction)
    private LocalDateTime updatedAt;
}
