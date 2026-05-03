package com.wiki.app.admin.dept.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminDepartmentResponse {
    private Long id;
    private String name;
    private Long parentId;
    private Long managerId;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

