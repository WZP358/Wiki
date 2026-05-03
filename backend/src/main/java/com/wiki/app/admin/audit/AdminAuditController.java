package com.wiki.app.admin.audit;

import com.wiki.app.admin.audit.dto.AdminDocEditLogResponse;
import com.wiki.app.admin.audit.dto.AdminDocResponse;
import com.wiki.app.admin.audit.dto.AdminDocViewLogResponse;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.doc.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final WikiDocumentRepository documentRepository;
    private final DocumentViewLogRepository viewLogRepository;
    private final DocumentEditLogRepository editLogRepository;

    public AdminAuditController(WikiDocumentRepository documentRepository,
                                DocumentViewLogRepository viewLogRepository,
                                DocumentEditLogRepository editLogRepository) {
        this.documentRepository = documentRepository;
        this.viewLogRepository = viewLogRepository;
        this.editLogRepository = editLogRepository;
    }

    @GetMapping("/docs")
    public ApiResponse<Page<AdminDocResponse>> docs(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Long kbId,
                                                    @RequestParam(required = false) Long ownerId,
                                                    @RequestParam(required = false) Boolean published,
                                                    @RequestParam(required = false) String visibility,
                                                    @RequestParam(required = false) Boolean deleted,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        DocVisibility visibilityEnum = null;
        if (visibility != null && !visibility.isBlank()) {
            try {
                visibilityEnum = DocVisibility.valueOf(visibility.trim().toUpperCase());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid visibility");
            }
        }
        Page<WikiDocument> result = documentRepository.adminSearch(
                keyword == null ? null : keyword.trim(),
                kbId,
                ownerId,
                published,
                visibilityEnum,
                deleted,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return ApiResponse.ok(result.map(this::toDocResponse));
    }

    @GetMapping("/doc-view-logs")
    public ApiResponse<Page<AdminDocViewLogResponse>> docViewLogs(@RequestParam(required = false) Long docId,
                                                                  @RequestParam(required = false) Long userId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        if (docId != null) {
            return ApiResponse.ok(viewLogRepository.findByDocIdOrderByCreatedAtDesc(docId, PageRequest.of(page, size))
                    .map(this::toViewLogResponse));
        }
        if (userId != null) {
            return ApiResponse.ok(viewLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                    .map(this::toViewLogResponse));
        }
        // default: no filter, show newest
        return ApiResponse.ok(viewLogRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toViewLogResponse));
    }

    @GetMapping("/doc-edit-logs")
    public ApiResponse<Page<AdminDocEditLogResponse>> docEditLogs(@RequestParam(required = false) Long docId,
                                                                  @RequestParam(required = false) Long userId,
                                                                  @RequestParam(required = false) String action,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        if (docId != null) {
            return ApiResponse.ok(editLogRepository.findByDocIdOrderByCreatedAtDesc(docId, PageRequest.of(page, size))
                    .map(this::toEditLogResponse));
        }
        if (userId != null) {
            return ApiResponse.ok(editLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                    .map(this::toEditLogResponse));
        }
        if (action != null && !action.isBlank()) {
            return ApiResponse.ok(editLogRepository.findByActionOrderByCreatedAtDesc(action.trim().toUpperCase(), PageRequest.of(page, size))
                    .map(this::toEditLogResponse));
        }
        return ApiResponse.ok(editLogRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toEditLogResponse));
    }

    private AdminDocResponse toDocResponse(WikiDocument d) {
        return AdminDocResponse.builder()
                .id(d.getId())
                .kbId(d.getKbId())
                .parentId(d.getParentId())
                .title(d.getTitle())
                .ownerId(d.getOwnerId())
                .visibility(d.getVisibility() == null ? null : d.getVisibility().name())
                .published(d.getPublished())
                .viewCount(d.getViewCount())
                .versionNo(d.getVersionNo())
                .deleted(d.getDeletedAt() != null)
                .deletedAt(d.getDeletedAt())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private AdminDocViewLogResponse toViewLogResponse(DocumentViewLog v) {
        return AdminDocViewLogResponse.builder()
                .id(v.getId())
                .docId(v.getDocId())
                .userId(v.getUserId())
                .username(v.getUsername())
                .ip(v.getIp())
                .userAgent(v.getUserAgent())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private AdminDocEditLogResponse toEditLogResponse(DocumentEditLog e) {
        return AdminDocEditLogResponse.builder()
                .id(e.getId())
                .docId(e.getDocId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .action(e.getAction())
                .titleBefore(e.getTitleBefore())
                .titleAfter(e.getTitleAfter())
                .contentLengthBefore(e.getContentLengthBefore())
                .contentLengthAfter(e.getContentLengthAfter())
                .ip(e.getIp())
                .commitMessage(e.getCommitMessage())
                .createdAt(e.getCreatedAt())
                .build();
    }
}

