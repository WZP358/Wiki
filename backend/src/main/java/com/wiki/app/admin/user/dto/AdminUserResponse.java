package com.wiki.app.admin.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String role;
    private Long departmentId;
    private String departmentName;
    private List<Long> teamIds;
    private List<String> teamNames;
    private boolean active;
    private boolean pendingAssignment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

