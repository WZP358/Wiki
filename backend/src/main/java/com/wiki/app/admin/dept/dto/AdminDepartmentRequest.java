package com.wiki.app.admin.dept.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminDepartmentRequest {
    @NotBlank
    private String name;
    private Long parentId;
    private Long managerId;
    private String description;
}

