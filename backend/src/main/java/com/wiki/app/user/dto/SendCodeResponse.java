package com.wiki.app.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendCodeResponse {
    private boolean testMode;
    private String code;
    private String message;
}
