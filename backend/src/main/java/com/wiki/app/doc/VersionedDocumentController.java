package com.wiki.app.doc;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.doc.dto.CreateDocumentRequest;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.doc.dto.DocumentTreeNode;
import com.wiki.app.doc.dto.DocumentVersionResponse;
import com.wiki.app.doc.dto.EditLockResponse;
import com.wiki.app.doc.dto.SaveDraftRequest;
import com.wiki.app.doc.dto.VersionDiffResponse;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/docs")
public class VersionedDocumentController {
    private final DocumentService documentService;

    public VersionedDocumentController(DocumentService documentService) {
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
    public ApiResponse<DocumentResponse> get(@PathVariable Long docId, HttpServletRequest httpRequest) {
        return ApiResponse.ok(documentService.get(
                docId,
                SecurityUtils.currentUser(),
                IpUtils.resolve(httpRequest),
                httpRequest.getHeader("User-Agent")
        ));
    }

    @PutMapping("/{docId}")
    public ApiResponse<DocumentResponse> update(@PathVariable Long docId,
                                                @RequestBody com.wiki.app.doc.dto.UpdateDocumentRequest request,
                                                HttpServletRequest httpRequest) {
        return ApiResponse.ok(documentService.update(docId, request, SecurityUtils.currentUser(), IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{docId}")
    public ApiResponse<Void> delete(@PathVariable Long docId, HttpServletRequest httpRequest) {
        documentService.delete(docId, SecurityUtils.currentUser(), IpUtils.resolve(httpRequest));
        return ApiResponse.ok("Document deleted", null);
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
        return ApiResponse.ok(documentService.rollback(docId, versionId, SecurityUtils.currentUser(), IpUtils.resolve(httpRequest)));
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
        return ApiResponse.ok("Edit lock released", null);
    }

    @PostMapping("/{docId}/draft")
    public ApiResponse<?> saveDraft(@PathVariable Long docId,
                                    @RequestBody SaveDraftRequest request) {
        return ApiResponse.ok(documentService.saveDraft(docId, request, SecurityUtils.currentUser()));
    }
}
