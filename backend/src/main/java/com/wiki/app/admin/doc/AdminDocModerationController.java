package com.wiki.app.admin.doc;

import com.wiki.app.admin.doc.dto.AdminDocActionRequest;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/docs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocModerationController {
    private final WikiDocumentRepository documentRepository;
    private final DocumentService documentService;
    private final OperationLogService operationLogService;

    public AdminDocModerationController(WikiDocumentRepository documentRepository,
                                        DocumentService documentService,
                                        OperationLogService operationLogService) {
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/action")
    @Transactional
    public ApiResponse<Void> action(@Valid @RequestBody AdminDocActionRequest request,
                                    HttpServletRequest httpRequest) {
        CurrentUser admin = SecurityUtils.currentUser();
        String ip = IpUtils.resolve(httpRequest);

        WikiDocument doc = documentRepository.findById(request.getDocId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Document not found"));

        if (request.getDeleted() != null) {
            if (Boolean.TRUE.equals(request.getDeleted())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "Administrators can restore deleted documents, but deletion is handled by knowledge base managers");
            }
            boolean wantDeleted = Boolean.TRUE.equals(request.getDeleted());
            doc.setDeletedAt(wantDeleted ? LocalDateTime.now() : null);
            documentRepository.save(doc);
            operationLogService.record(admin.getUserId(), admin.getUsername(),
                    wantDeleted ? "ADMIN_SOFT_DELETE_DOC" : "ADMIN_RESTORE_DOC",
                    "DOC", doc.getId().toString(), ip, request.getReason());
        }

        // publish toggle
        if (request.getPublished() != null) {
            doc.setPublished(Boolean.TRUE.equals(request.getPublished()));
            documentRepository.save(doc);
            operationLogService.record(admin.getUserId(), admin.getUsername(),
                    Boolean.TRUE.equals(doc.getPublished()) ? "ADMIN_PUBLISH_DOC" : "ADMIN_UNPUBLISH_DOC",
                    "DOC", doc.getId().toString(), ip, request.getReason());
        }

        if (Boolean.TRUE.equals(request.getPurge())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Documents are soft-deleted only and cannot be purged");
        }

        return ApiResponse.ok(null);
    }
}

