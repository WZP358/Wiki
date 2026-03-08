package com.wiki.app.share;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.share.dto.ShareResponse;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shares")
public class ShareController {
    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/docs/{docId}")
    public ApiResponse<ShareResponse> create(@PathVariable Long docId, HttpServletRequest request) {
        return ApiResponse.ok(shareService.createDocShare(docId, SecurityUtils.currentUser(), IpUtils.resolve(request)));
    }

    @GetMapping("/public/{token}")
    public ApiResponse<DocumentResponse> publicView(@PathVariable String token) {
        return ApiResponse.ok(shareService.publicView(token));
    }
}
