package com.wiki.app.template;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.template.dto.CreateTemplateRequest;
import com.wiki.app.template.dto.TemplateResponse;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class DocumentTemplateController {
    private final DocumentTemplateService templateService;

    public DocumentTemplateController(DocumentTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ApiResponse<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request,
                                               HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(templateService.create(request, user, IpUtils.resolve(httpRequest)));
    }

    @GetMapping
    public ApiResponse<List<TemplateResponse>> list(@RequestParam(required = false) Long kbId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(templateService.listAvailable(kbId, user));
    }

    @GetMapping("/category/{category}")
    public ApiResponse<List<TemplateResponse>> listByCategory(@PathVariable String category,
                                                              @RequestParam(required = false) Long kbId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(templateService.listByCategory(category, kbId, user));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<TemplateResponse> get(@PathVariable Long templateId) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(templateService.get(templateId, user));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<TemplateResponse> update(@PathVariable Long templateId,
                                               @Valid @RequestBody CreateTemplateRequest request,
                                               HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        return ApiResponse.ok(templateService.update(templateId, request, user, IpUtils.resolve(httpRequest)));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable Long templateId,
                                   HttpServletRequest httpRequest) {
        CurrentUser user = SecurityUtils.currentUser();
        templateService.delete(templateId, user, IpUtils.resolve(httpRequest));
        return ApiResponse.ok("模板已删除", null);
    }

    @PostMapping("/{templateId}/use")
    public ApiResponse<Void> incrementUseCount(@PathVariable Long templateId) {
        templateService.incrementUseCount(templateId);
        return ApiResponse.ok(null);
    }
}
