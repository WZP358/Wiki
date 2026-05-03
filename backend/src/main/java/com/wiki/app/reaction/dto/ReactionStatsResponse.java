package com.wiki.app.reaction.dto;

import com.wiki.app.reaction.ReactionType;
import lombok.Data;

import java.util.Map;

@Data
public class ReactionStatsResponse {
    private Map<ReactionType, Long> counts;
    private Map<ReactionType, Boolean> userReacted;
}
