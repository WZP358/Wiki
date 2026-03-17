package com.wiki.app.kb.dto;

import com.wiki.app.kb.MemberRole;
import lombok.Data;

@Data
public class InviteMemberRequest {
    /**
     * 兼容两种邀请方式：
     * - userId：直接按用户ID邀请
     * - usernameOrEmail：按用户名或邮箱邀请
     */
    private Long userId;

    private String usernameOrEmail;

    private MemberRole role;
}
