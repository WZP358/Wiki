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

    @PutMapping("/{kbId}")
    public ApiResponse<KnowledgeBaseResponse> update(@PathVariable Long kbId,
                                                     @Valid @RequestBody CreateKnowledgeBaseRequest request,
                                                     HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(knowledgeBaseService.update(kbId, request, user, IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{kbId}")
    public ApiResponse<Void> delete(@PathVariable Long kbId, HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        knowledgeBaseService.delete(kbId, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok(null);
    }

    @GetMapping("/{kbId}")
    public ApiResponse<KnowledgeBaseResponse> get(@PathVariable Long kbId) {
        return ApiResponse.ok(knowledgeBaseService.getForCurrent(kbId, SecurityUtils.currentUser()));
    }

    @GetMapping("/user/{userId}/public")
    public ApiResponse<List<KnowledgeBaseResponse>> publicByUser(@PathVariable Long userId) {
        return ApiResponse.ok(knowledgeBaseService.listPublicByUser(userId));
    }

    @GetMapping("/search")
    public ApiResponse<List<KnowledgeBaseResponse>> search(@RequestParam String keyword) {
        return ApiResponse.ok(knowledgeBaseService.searchVisibleKbs(keyword, SecurityUtils.currentUser()));
    }

    @GetMapping("/by-department")
    public ApiResponse<List<KnowledgeBaseResponse>> byDepartment(@RequestParam Long deptId) {
        return ApiResponse.ok(knowledgeBaseService.listByDepartment(deptId, SecurityUtils.currentUser()));
    }

    @GetMapping("/{kbId}/members")
    public ApiResponse<List<KbMemberDetailResponse>> members(@PathVariable Long kbId) {
        return ApiResponse.ok(knowledgeBaseService.listMembers(kbId, SecurityUtils.currentUser()));
    }

    @PostMapping("/{kbId}/members")
    public ApiResponse<MemberResponse> inviteOrUpdateMember(@PathVariable Long kbId,
                                                            @Valid @RequestBody InviteMemberRequest request,
                                                            HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(knowledgeBaseService.upsertMember(kbId, request, user, IpUtils.resolve(httpRequest)));
    }
}