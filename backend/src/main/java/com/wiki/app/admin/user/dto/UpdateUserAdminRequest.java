package com.wiki.app.admin.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Admin-only user mutations. Any field can be null to mean "no change".
 */
@Data
public class UpdateUserAdminRequest {
    @NotNull
    private Long userId;
    private String role; // "ADMIN" / "USER"
    private Long departmentId;
    private List<Long> teamIds;
    private Boolean active; // true -> enable, false -> disable (soft delete)
}

