package com.wiki.app.kb;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.kb.dto.*;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/kbs")
public class KnowledgeBaseController {
    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> create(@Valid @RequestBody CreateKnowledgeBaseRequest request,
                                                     HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(knowledgeBaseService.create(request, user, IpUtils.resolve(httpRequest)));
    }

    @GetMapping("/mine")
    public ApiResponse<List<KnowledgeBaseResponse>> mine() {
        return ApiResponse.ok(knowledgeBaseService.listMine(SecurityUtils.currentUser()));
    }

    @GetMapping("/user/{userId}/public")
    public ApiResponse<List<KnowledgeBaseResponse>> publicByUser(@PathVariable Long userId) {
        return ApiResponse.ok(knowledgeBaseService.listPublicByUser(userId));
    }

