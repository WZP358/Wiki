package com.wiki.app.admin.user.dto;

import com.wiki.app.kb.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPendingUserRequest {
    @NotNull
    private Long userId;

    /**
     * Backward-compatible primary team field. New clients should send teamIds.
     */
    private Long departmentId;

    private List<Long> teamIds;

    private Long kbId;

    private MemberRole memberRole;
}
