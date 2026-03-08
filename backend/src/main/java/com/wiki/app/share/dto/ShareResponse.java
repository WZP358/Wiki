package com.wiki.app.share.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ShareResponse {
    private String token;
    private String url;
}
