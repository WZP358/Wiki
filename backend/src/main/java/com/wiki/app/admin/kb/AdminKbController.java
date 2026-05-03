package com.wiki.app.admin.kb;

import com.wiki.app.admin.kb.dto.AdminKbActionRequest;
import com.wiki.app.admin.kb.dto.AdminKbResponse;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseMemberSyncService;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseType;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/kbs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKbController {

    private final KnowledgeBaseRepository kbRepository;
    private final DepartmentRepository departmentRepository;
    private final OperationLogService operationLogService;
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public AdminKbController(KnowledgeBaseRepository kbRepository,
                             DepartmentRepository departmentRepository,
                             OperationLogService operationLogService,
                             KnowledgeBaseMemberSyncService memberSyncService) {
        this.kbRepository = kbRepository;
        this.departmentRepository = departmentRepository;
        this.operationLogService = operationLogService;
        this.memberSyncService = memberSyncService;
    }

    @GetMapping
    public ApiResponse<Page<AdminKbResponse>> list(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String type,
                                                   @RequestParam(required = false) Long ownerId,
                                                   @RequestParam(required = false) Boolean deleted,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        KnowledgeBaseType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = KnowledgeBaseType.from(type);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid type");
            }
        }

        Page<KnowledgeBase> result = kbRepository.adminSearch(
                keyword == null ? null : keyword.trim(),
                typeEnum,
                ownerId,
                deleted,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return ApiResponse.ok(result.map(this::toResponse));
    }

    @GetMapping("/{kbId}")
    public ApiResponse<AdminKbResponse> get(@PathVariable Long kbId) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
        return ApiResponse.ok(toResponse(kb));
    }

    @PostMapping("/action")
    @Transactional
    public ApiResponse<Void> action(@Valid @RequestBody AdminKbActionRequest request,
                                    HttpServletRequest httpRequest) {
        CurrentUser admin = SecurityUtils.currentUser();
        String ip = IpUtils.resolve(httpRequest);

        KnowledgeBase kb = kbRepository.findById(request.getKbId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));

        if (request.getDeleted() != null) {
            boolean wantDeleted = Boolean.TRUE.equals(request.getDeleted());
            kb.setDeletedAt(wantDeleted ? LocalDateTime.now() : null);
            kbRepository.save(kb);
            memberSyncService.syncKnowledgeBaseAutoReaders(kb);
            operationLogService.record(admin.getUserId(), admin.getUsername(),
                    wantDeleted ? "ADMIN_SOFT_DELETE_KB" : "ADMIN_RESTORE_KB",
                    "KB", kb.getId().toString(), ip, request.getReason());
        }

        if (Boolean.TRUE.equals(request.getPurge())) {
            if (!Boolean.TRUE.equals(request.getConfirmed())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Purge requires confirmed=true");
            }
            // Hard delete: docs will be cascaded by FK (documents.kb_id -> knowledge_bases.id ON DELETE CASCADE)
            kbRepository.delete(kb);
            operationLogService.record(admin.getUserId(), admin.getUsername(),
                    "ADMIN_PURGE_KB", "KB", request.getKbId().toString(), ip, request.getReason());
        }

        return ApiResponse.ok(null);
    }

    private AdminKbResponse toResponse(KnowledgeBase kb) {
        return AdminKbResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .type(kb.getType() == null ? null : kb.getType().name())
                .ownerId(kb.getOwnerId())
                .teamId(kb.getTeamId())
                .teamName(teamName(kb.getTeamId()))
                .description(kb.getDescription())
                .deleted(kb.getDeletedAt() != null)
                .deletedAt(kb.getDeletedAt())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }

    private String teamName(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return departmentRepository.findById(teamId)
                .filter(team -> team.getDeletedAt() == null)
                .map(Department::getName)
                .orElse(null);
    }
}

