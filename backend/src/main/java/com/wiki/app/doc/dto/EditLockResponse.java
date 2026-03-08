package com.wiki.app.doc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EditLockResponse {
    private boolean locked;
    private String owner;
    private String message;
}
