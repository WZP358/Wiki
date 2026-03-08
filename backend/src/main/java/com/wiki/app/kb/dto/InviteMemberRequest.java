package com.wiki.app.kb.dto;

import com.wiki.app.kb.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotNull
    private Long userId;

    @NotNull
    private MemberRole role;
}
