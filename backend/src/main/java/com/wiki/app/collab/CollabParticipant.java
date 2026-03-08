package com.wiki.app.collab;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CollabParticipant {
    private String sessionId;
    private Long userId;
    private String username;
    private String avatarUrl;
    private Integer cursorStart;
    private Integer cursorEnd;
}
