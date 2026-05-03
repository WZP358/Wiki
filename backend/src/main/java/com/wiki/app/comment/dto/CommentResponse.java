package com.wiki.app.comment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentResponse {
    private String id;
    private String documentId;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private String parentId;
    private String content;
    private Integer likeCount;
    private Boolean isResolved;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponse> replies;
}
