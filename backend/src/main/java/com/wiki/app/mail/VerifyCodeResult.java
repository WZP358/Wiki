package com.wiki.app.mail;

import lombok.Data;

@Data
public class VerifyCodeResult {
    private boolean testMode;
    private String code;
    private String message;
}
