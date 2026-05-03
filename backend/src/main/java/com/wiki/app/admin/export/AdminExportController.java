package com.wiki.app.admin.export;

import com.wiki.app.common.ApiResponse;
import com.wiki.app.doc.DocumentEditLog;
import com.wiki.app.doc.DocumentEditLogRepository;
import com.wiki.app.doc.DocumentViewLog;
import com.wiki.app.doc.DocumentViewLogRepository;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.log.OperationLog;
import com.wiki.app.log.OperationLogRepository;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/export")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExportController {

    private final UserRepository userRepository;
    private final WikiDocumentRepository documentRepository;
    private final OperationLogRepository operationLogRepository;
    private final DocumentViewLogRepository viewLogRepository;
    private final DocumentEditLogRepository editLogRepository;

    public AdminExportController(UserRepository userRepository,
                                 WikiDocumentRepository documentRepository,
                                 OperationLogRepository operationLogRepository,
                                 DocumentViewLogRepository viewLogRepository,
                                 DocumentEditLogRepository editLogRepository) {
        this.userRepository = userRepository;
        this.documentRepository = documentRepository;
        this.operationLogRepository = operationLogRepository;
        this.viewLogRepository = viewLogRepository;
        this.editLogRepository = editLogRepository;
    }

    @GetMapping("/users.csv")
    public void exportUsers(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Long departmentId,
                            @RequestParam(required = false) Boolean active,
                            HttpServletResponse response) throws Exception {
        UserRole roleEnum = null;
        if (role != null && !role.isBlank()) {
            roleEnum = UserRole.valueOf(role.trim().toUpperCase());
        }
        String file = "users.csv";
        prepareCsv(response, file);
        try (PrintWriter out = response.getWriter()) {
            out.println("id,username,nickname,email,phone,role,departmentId,active,createdAt,updatedAt");
            int page = 0;
            while (true) {
                var result = userRepository.adminSearch(
                        keyword == null ? null : keyword.trim(),
                        roleEnum,
                        departmentId,
                        active,
                        PageRequest.of(page, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"))
                );
                List<UserAccount> content = result.getContent();
                for (UserAccount u : content) {
                    out.println(csv(u.getId())
                            + "," + csv(u.getUsername())
                            + "," + csv(u.getNickname())
                            + "," + csv(u.getEmail())
                            + "," + csv(u.getPhone())
                            + "," + csv(u.getRole() == null ? null : u.getRole().name())
                            + "," + csv(u.getDepartmentId())
                            + "," + csv(u.getDeletedAt() == null)
                            + "," + csv(u.getCreatedAt())
                            + "," + csv(u.getUpdatedAt()));
                }
                if (!result.hasNext()) break;
                page++;
            }
        }
    }

    @GetMapping("/docs.csv")
    public void exportDocs(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long kbId,
                           @RequestParam(required = false) Long ownerId,
                           @RequestParam(required = false) Boolean published,
                           @RequestParam(required = false) String visibility,
                           @RequestParam(required = false) Boolean deleted,
                           HttpServletResponse response) throws Exception {
        String file = "documents.csv";
        prepareCsv(response, file);
        try (PrintWriter out = response.getWriter()) {
            out.println("id,kbId,ownerId,title,visibility,published,deleted,deletedAt,createdAt,updatedAt,viewCount,versionNo");
            int page = 0;
            while (true) {
                var result = documentRepository.adminSearch(
                        keyword == null ? null : keyword.trim(),
                        kbId,
                        ownerId,
                        published,
                        visibility == null || visibility.isBlank() ? null : com.wiki.app.doc.DocVisibility.valueOf(visibility.trim().toUpperCase()),
                        deleted,
                        PageRequest.of(page, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"))
                );
                for (WikiDocument d : result.getContent()) {
                    out.println(csv(d.getId())
                            + "," + csv(d.getKbId())
                            + "," + csv(d.getOwnerId())
                            + "," + csv(d.getTitle())
                            + "," + csv(d.getVisibility() == null ? null : d.getVisibility().name())
                            + "," + csv(d.getPublished())
                            + "," + csv(d.getDeletedAt() != null)
                            + "," + csv(d.getDeletedAt())
                            + "," + csv(d.getCreatedAt())
                            + "," + csv(d.getUpdatedAt())
                            + "," + csv(d.getViewCount())
                            + "," + csv(d.getVersionNo()));
                }
                if (!result.hasNext()) break;
                page++;
            }
        }
    }

    @GetMapping("/operation-logs.csv")
    public void exportOperationLogs(@RequestParam(required = false) Long userId,
                                    @RequestParam(required = false) String action,
                                    @RequestParam(required = false) String targetType,
                                    @RequestParam(required = false) String targetId,
                                    @RequestParam(required = false) String ip,
                                    @RequestParam(required = false) String fromTime,
                                    @RequestParam(required = false) String toTime,
                                    HttpServletResponse response) throws Exception {
        String file = "operation_logs.csv";
        prepareCsv(response, file);
        LocalDateTime from = parseTime(fromTime);
        LocalDateTime to = parseTime(toTime);
        try (PrintWriter out = response.getWriter()) {
            out.println("id,createdAt,userId,username,action,targetType,targetId,ip,detail");
            int page = 0;
            while (true) {
                var result = operationLogRepository.adminSearch(
                        userId,
                        action == null ? null : action.trim(),
                        targetType == null ? null : targetType.trim(),
                        targetId == null ? null : targetId.trim(),
                        ip == null ? null : ip.trim(),
                        from,
                        to,
                        PageRequest.of(page, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
                for (OperationLog l : result.getContent()) {
                    out.println(csv(l.getId())
                            + "," + csv(l.getCreatedAt())
                            + "," + csv(l.getUserId())
                            + "," + csv(l.getUsername())
                            + "," + csv(l.getAction())
                            + "," + csv(l.getTargetType())
                            + "," + csv(l.getTargetId())
                            + "," + csv(l.getIp())
                            + "," + csv(l.getDetail()));
                }
                if (!result.hasNext()) break;
                page++;
            }
        }
    }

    @GetMapping("/doc-view-logs.csv")
    public void exportDocViewLogs(@RequestParam(required = false) Long docId,
                                  @RequestParam(required = false) Long userId,
                                  HttpServletResponse response) throws Exception {
        String file = "doc_view_logs.csv";
        prepareCsv(response, file);
        try (PrintWriter out = response.getWriter()) {
            out.println("id,createdAt,docId,userId,username,ip,userAgent");
            int page = 0;
            while (true) {
                var pageable = PageRequest.of(page, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
                var result = (docId != null) ? viewLogRepository.findByDocIdOrderByCreatedAtDesc(docId, pageable)
                        : (userId != null) ? viewLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        : viewLogRepository.findAll(pageable);
                for (DocumentViewLog v : result.getContent()) {
                    out.println(csv(v.getId())
                            + "," + csv(v.getCreatedAt())
                            + "," + csv(v.getDocId())
                            + "," + csv(v.getUserId())
                            + "," + csv(v.getUsername())
                            + "," + csv(v.getIp())
                            + "," + csv(v.getUserAgent()));
                }
                if (!result.hasNext()) break;
                page++;
            }
        }
    }

    @GetMapping("/doc-edit-logs.csv")
    public void exportDocEditLogs(@RequestParam(required = false) Long docId,
                                  @RequestParam(required = false) Long userId,
                                  @RequestParam(required = false) String action,
                                  HttpServletResponse response) throws Exception {
        String file = "doc_edit_logs.csv";
        prepareCsv(response, file);
        try (PrintWriter out = response.getWriter()) {
            out.println("id,createdAt,docId,userId,username,action,titleBefore,titleAfter,lenBefore,lenAfter,ip,commitMessage");
            int page = 0;
            while (true) {
                var pageable = PageRequest.of(page, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
                var result = (docId != null) ? editLogRepository.findByDocIdOrderByCreatedAtDesc(docId, pageable)
                        : (userId != null) ? editLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                        : (action != null && !action.isBlank()) ? editLogRepository.findByActionOrderByCreatedAtDesc(action.trim().toUpperCase(), pageable)
                        : editLogRepository.findAll(pageable);
                for (DocumentEditLog e : result.getContent()) {
                    out.println(csv(e.getId())
                            + "," + csv(e.getCreatedAt())
                            + "," + csv(e.getDocId())
                            + "," + csv(e.getUserId())
                            + "," + csv(e.getUsername())
                            + "," + csv(e.getAction())
                            + "," + csv(e.getTitleBefore())
                            + "," + csv(e.getTitleAfter())
                            + "," + csv(e.getContentLengthBefore())
                            + "," + csv(e.getContentLengthAfter())
                            + "," + csv(e.getIp())
                            + "," + csv(e.getCommitMessage()));
                }
                if (!result.hasNext()) break;
                page++;
            }
        }
    }

    // For UI testing: quick health ping
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("ok");
    }

    private void prepareCsv(HttpServletResponse response, String filename) throws Exception {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=utf-8");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        // UTF-8 BOM for Excel
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        boolean needQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        s = s.replace("\"", "\"\"");
        return needQuote ? "\"" + s + "\"" : s;
    }

    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}

