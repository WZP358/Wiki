package com.wiki.app.kb.dto;

import com.wiki.app.kb.MemberRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KbMemberDetailResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String displayName;
    private String avatarUrl;
    private Long departmentId;
    private String departmentName;
    private String positionName;
    private MemberRole role;
}

