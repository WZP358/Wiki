package com.wiki.app.comment;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.comment.dto.CommentResponse;
import com.wiki.app.comment.dto.CreateCommentRequest;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs/{docId}/comments")
public class DocumentCommentController {
    private final DocumentCommentService commentService;

    public DocumentCommentController(DocumentCommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ApiResponse<CommentResponse> create(@PathVariable Long docId,
                                              @Valid @RequestBody CreateCommentRequest request,
                                              HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(commentService.create(docId, request, user, IpUtils.resolve(httpRequest)));
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(@PathVariable Long docId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(commentService.listByDocument(docId, user));
    }

    @PutMapping("/{commentId}")
    public ApiResponse<CommentResponse> update(@PathVariable Long docId,
                                              @PathVariable Long commentId,
                                              @Valid @RequestBody CreateCommentRequest request,
                                              HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(commentService.update(commentId, request, user, IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@PathVariable Long docId,
                                   @PathVariable Long commentId,
                                   HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        commentService.delete(commentId, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("评论已删除", null);
    }

    @PostMapping("/{commentId}/resolve")
    public ApiResponse<Void> resolve(@PathVariable Long docId,
                                    @PathVariable Long commentId,
                                    HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        commentService.resolve(commentId, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("评论已解决", null);
    }

    @GetMapping("/count")
    public ApiResponse<Long> count(@PathVariable Long docId) {
        return ApiResponse.ok(commentService.countByDocument(docId));
    }
}
