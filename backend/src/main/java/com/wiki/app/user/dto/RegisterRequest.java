package com.wiki.app.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    private String contact;

    private String email;

    private String phone;

    private String avatarUrl;

    @NotBlank
    private String code;

    @AssertTrue(message = "Contact is required")
    public boolean isContactProvided() {
        return StringUtils.hasText(contact) || StringUtils.hasText(email) || StringUtils.hasText(phone);
    }
}
