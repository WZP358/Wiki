package com.wiki.app.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String avatarUrl;
    private String email;
    private String emailCode;
    private String phone;
    private String phoneCode;
}
