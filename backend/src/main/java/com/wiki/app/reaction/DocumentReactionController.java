package com.wiki.app.reaction;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.reaction.dto.ReactionStatsResponse;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/docs/{docId}/reactions")
public class DocumentReactionController {
    private final DocumentReactionService reactionService;

    public DocumentReactionController(DocumentReactionService reactionService) {
        this.reactionService = reactionService;
    }

    @PostMapping("/{reactionType}")
    public ApiResponse<Void> toggle(@PathVariable Long docId,
                                   @PathVariable ReactionType reactionType,
                                   HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        reactionService.toggle(docId, reactionType, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("操作成功", null);
    }

    @GetMapping
    public ApiResponse<ReactionStatsResponse> getStats(@PathVariable Long docId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(reactionService.getStats(docId, user));
    }
}
