package com.wiki.app.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicUserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String departmentName;
}

