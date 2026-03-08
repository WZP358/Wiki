package com.wiki.app.doc;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.doc.dto.*;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ApiResponse<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request,
                                                HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(documentService.create(request, user, IpUtils.resolve(httpRequest)));
    }

    @GetMapping("/tree")
    public ApiResponse<List<DocumentTreeNode>> tree(@RequestParam Long kbId) {
        return ApiResponse.ok(documentService.tree(kbId, SecurityUtils.currentUser()));
    }

    @GetMapping("/{docId}")
    public ApiResponse<DocumentResponse> get(@PathVariable Long docId) {
        return ApiResponse.ok(documentService.get(docId, SecurityUtils.currentUser()));
    }

    @PutMapping("/{docId}")
    public ApiResponse<DocumentResponse> update(@PathVariable Long docId,
                                                @RequestBody UpdateDocumentRequest request,
                                                HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(documentService.update(docId, request, user, IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{docId}")
    public ApiResponse<Void> delete(@PathVariable Long docId, HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        documentService.delete(docId, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("删除成功", null);
    }

    @GetMapping("/recycle")
    public ApiResponse<List<DocumentResponse>> recycle() {
        return ApiResponse.ok(documentService.recycle(SecurityUtils.currentUser()));
    }

    @PostMapping("/{docId}/restore")
    public ApiResponse<DocumentResponse> restore(@PathVariable Long docId, HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(documentService.restore(docId, user, IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{docId}/purge")
    public ApiResponse<Void> purge(@PathVariable Long docId,
                                   @RequestParam(defaultValue = "false") boolean confirmed,
                                   HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        documentService.purge(docId, user, confirmed, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("已彻底删除", null);
    }

    @GetMapping("/{docId}/versions")
    public ApiResponse<List<DocumentVersionResponse>> versions(@PathVariable Long docId) {
        return ApiResponse.ok(documentService.versions(docId, SecurityUtils.currentUser()));
    }

    @GetMapping("/{docId}/versions/diff")
    public ApiResponse<VersionDiffResponse> diffVersions(@PathVariable Long docId,
                                                         @RequestParam Long leftVersionId,
                                                         @RequestParam Long rightVersionId) {
        return ApiResponse.ok(documentService.diffVersions(docId, leftVersionId, rightVersionId, SecurityUtils.currentUser()));
    }

    @PostMapping("/{docId}/rollback/{versionId}")
    public ApiResponse<DocumentResponse> rollback(@PathVariable Long docId,
                                                  @PathVariable Long versionId,
                                                  HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(documentService.rollback(docId, versionId, user, IpUtils.resolve(httpRequest)));
    }

    @GetMapping("/search")
    public ApiResponse<List<DocumentResponse>> search(@RequestParam Long kbId,
                                                      @RequestParam String keyword) {
        return ApiResponse.ok(documentService.search(kbId, keyword, SecurityUtils.currentUser()));
    }

    @GetMapping("/latest")
    public ApiResponse<List<DocumentResponse>> latest(@RequestParam Long kbId) {
        return ApiResponse.ok(documentService.latest(kbId, SecurityUtils.currentUser()));
    }

    @GetMapping("/hot")
    public ApiResponse<List<DocumentResponse>> hot(@RequestParam Long kbId) {
        return ApiResponse.ok(documentService.hot(kbId, SecurityUtils.currentUser()));
    }

    @PostMapping("/{docId}/lock")
    public ApiResponse<EditLockResponse> lock(@PathVariable Long docId) {
        return ApiResponse.ok(documentService.lock(docId, SecurityUtils.currentUser()));
    }

    @DeleteMapping("/{docId}/lock")
    public ApiResponse<Void> unlock(@PathVariable Long docId) {
        documentService.unlock(docId, SecurityUtils.currentUser());
        return ApiResponse.ok("已释放编辑锁", null);
    }

    @PostMapping("/{docId}/draft")
    public ApiResponse<DraftResponse> saveDraft(@PathVariable Long docId,
                                                @RequestBody SaveDraftRequest request) {
        return ApiResponse.ok(documentService.saveDraft(docId, request, SecurityUtils.currentUser()));
    }

    @GetMapping("/{docId}/draft")
    public ApiResponse<DraftResponse> getDraft(@PathVariable Long docId) {
        return ApiResponse.ok(documentService.draft(docId, SecurityUtils.currentUser()));
    }
}
