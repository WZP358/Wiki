package com.wiki.app.admin.doc.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminDocActionRequest {
    @NotNull
    private Long docId;
    private Boolean published; // optional
    private Boolean deleted;   // optional (soft delete toggle)
    private Boolean purge;     // optional (true means purge, requires confirmed)
    private Boolean confirmed; // required for purge
    private String reason;
}

