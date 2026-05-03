package com.wiki.app.admin.kb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminKbActionRequest {
    @NotNull
    private Long kbId;
    private Boolean deleted;   // soft delete toggle
    private Boolean purge;     // delete permanently (cascade docs)
    private Boolean confirmed; // required for purge
    private String reason;
}

