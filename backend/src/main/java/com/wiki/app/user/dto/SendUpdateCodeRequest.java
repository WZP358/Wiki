package com.wiki.app.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendUpdateCodeRequest {
    @NotBlank
    private String target;

    @NotBlank
    @Pattern(regexp = "EMAIL|PHONE")
    private String type;
}
