package com.wiki.app.kb.dto;

import com.wiki.app.kb.MemberRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberResponse {
    private Long userId;
    private MemberRole role;
}
