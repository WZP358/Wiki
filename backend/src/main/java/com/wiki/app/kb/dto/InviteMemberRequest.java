package com.wiki.app.kb.dto;

import com.wiki.app.kb.MemberRole;
import lombok.Data;

@Data
public class InviteMemberRequest {
    /**
     * Supports two invitation styles:
     * - userId: invite by user id
     * - usernameOrEmail: invite by username or email
     */
    private Long userId;

    private String usernameOrEmail;

    private MemberRole role;
}
