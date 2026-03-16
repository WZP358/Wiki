package com.wiki.app.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteDocumentResponse {
    private Long id;
    private Long docId;
    private String docTitle;
    private Long kbId;
    private String kbName;
    private String createdAt;
}
