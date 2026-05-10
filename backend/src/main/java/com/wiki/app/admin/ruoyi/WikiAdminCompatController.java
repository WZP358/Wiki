package com.wiki.app.admin.ruoyi;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.doc.DocVisibility;
import com.wiki.app.doc.DocumentService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.doc.dto.CreateDocumentRequest;
import com.wiki.app.doc.dto.DocumentResponse;
import com.wiki.app.doc.dto.UpdateDocumentRequest;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseMember;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseMemberSyncService;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseType;
import com.wiki.app.kb.MemberRole;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.template.DocumentTemplate;
import com.wiki.app.template.DocumentTemplateRepository;
import com.wiki.app.user.IpUtils;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class WikiAdminCompatController {
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final WikiDocumentRepository docRepository;
    private final DocumentService documentService;
    private final DocumentTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public WikiAdminCompatController(KnowledgeBaseRepository kbRepository,
                                     KnowledgeBaseMemberRepository memberRepository,
                                     WikiDocumentRepository docRepository,
                                     DocumentService documentService,
                                     DocumentTemplateRepository templateRepository,
                                     UserRepository userRepository,
                                     DepartmentRepository departmentRepository,
                                     SnowflakeIdGenerator idGenerator,
                                     KnowledgeBaseMemberSyncService memberSyncService) {
        this.kbRepository = kbRepository;
        this.memberRepository = memberRepository;
        this.docRepository = docRepository;
        this.documentService = documentService;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.idGenerator = idGenerator;
        this.memberSyncService = memberSyncService;
    }

    @GetMapping("/api/wiki/admin/kb/list")
    public Map<String, Object> listKb(@RequestParam Map<String, String> params) {
        Page<KnowledgeBase> page = kbRepository.adminSearch(
                firstNonBlank(params.get("kbName"), params.get("keyword")),
                parseKbType(firstNonBlank(params.get("type"), params.get("visibility"))),
                parseLong(params.get("ownerId")),
                parseDeletedFromStatus(params.get("status")),
                pageRequest(params, "updatedAt")
        );
        return table(page.map(this::kbRow));
    }

    @GetMapping("/api/wiki/admin/kb/{kbId}")
    public Map<String, Object> getKb(@PathVariable Long kbId) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        return okData(kbRow(kb));
    }

    @PostMapping("/api/wiki/admin/kb")
    @Transactional
    public Map<String, Object> addKb(@RequestBody Map<String, Object> body) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(idGenerator.nextId());
        kb.setName(firstNonBlank(stringValue(body.get("kbName")), stringValue(body.get("name"))));
        kb.setDescription(stringValue(body.get("description")));
        kb.setType(parseKbType(firstNonBlank(stringValue(body.get("visibility")), stringValue(body.get("type")))));
        kb.setTeamId(resolveTeamId(kb.getType(), parseLong(firstNonBlank(stringValue(body.get("teamId")), stringValue(body.get("departmentId")), stringValue(body.get("deptId")))), currentUser));
        kb.setOwnerId(currentUser.getUserId());
        if (kb.getName() == null || kb.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "知识库名称不能为空");
        }
        if (kb.getType() == null) {
            kb.setType(KnowledgeBaseType.COMPANY);
        }
        kbRepository.save(kb);
        memberSyncService.syncKnowledgeBaseAutoReaders(kb);
        return okMessage("操作成功");
    }

    @PutMapping("/api/wiki/admin/kb")
    @Transactional
    public Map<String, Object> updateKb(@RequestBody Map<String, Object> body) {
        Long kbId = parseLong(firstNonBlank(stringValue(body.get("kbId")), stringValue(body.get("id"))));
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        String name = firstNonBlank(stringValue(body.get("kbName")), stringValue(body.get("name")));
        if (name != null && !name.isBlank()) {
            kb.setName(name);
        }
        kb.setDescription(stringValue(body.get("description")));
        KnowledgeBaseType type = parseKbType(firstNonBlank(stringValue(body.get("visibility")), stringValue(body.get("type"))));
        if (type != null) {
            kb.setType(type);
            kb.setTeamId(resolveTeamId(type, parseLong(firstNonBlank(stringValue(body.get("teamId")), stringValue(body.get("departmentId")), stringValue(body.get("deptId")))), SecurityUtils.currentUser()));
        }
        String status = stringValue(body.get("status"));
        if (status != null) {
            kb.setDeletedAt("disabled".equals(status) ? LocalDateTime.now() : null);
        }
        kbRepository.save(kb);
        memberSyncService.syncKnowledgeBaseAutoReaders(kb);
        return okMessage("操作成功");
    }

    @DeleteMapping("/api/wiki/admin/kb/{kbIds}")
    @Transactional
    public Map<String, Object> deleteKb(@PathVariable String kbIds) {
        for (String id : kbIds.split(",")) {
            Long kbId = parseLong(id);
            if (kbId != null) {
                kbRepository.findById(kbId).ifPresent(kb -> {
                    kb.setDeletedAt(LocalDateTime.now());
                    kbRepository.save(kb);
                });
            }
        }
        return okMessage("删除成功");
    }

    @GetMapping("/api/wiki/admin/kb/{kbId}/permission")
    public Map<String, Object> getKbPermission(@PathVariable Long kbId) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "知识库不存在"));
        CurrentUser user = SecurityUtils.currentUser();
        Map<String, Object> data = row(
                "currentRole", user.getUserId().equals(kb.getOwnerId()) ? "owner" : "admin",
                "canEdit", true,
                "canManageMembers", true,
                "canTransferOwner", true,
                "currentUserId", user.getUserId(),
                "ownerId", kb.getOwnerId(),
                "ownerName", userName(kb.getOwnerId())
        );
        return okData(data);
    }

    @GetMapping("/api/wiki/admin/kb/member/list")
    public Map<String, Object> listKbMember(@RequestParam Map<String, String> params) {
        Long kbId = parseLong(params.get("kbId"));
        if (kbId == null) {
            return table(List.of(), 0);
        }
        KnowledgeBase kb = kbRepository.findById(kbId).orElse(null);
        if (kb == null) {
            return table(List.of(), 0);
        }
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        rows.add(memberRow(null, kb.getId(), kb.getOwnerId(), "owner"));
        for (KnowledgeBaseMember member : memberRepository.findByKbIdAndDeletedAtIsNull(kbId)) {
            rows.add(memberRow(member.getId(), member.getKbId(), member.getUserId(), roleCode(member.getRole())));
        }
        return table(rows, rows.size());
    }

    @GetMapping("/api/wiki/admin/document/list")
    public Map<String, Object> listDocument(@RequestParam Map<String, String> params) {
        Page<WikiDocument> page = docRepository.adminSearch(
                firstNonBlank(params.get("title"), params.get("keyword")),
                parseLong(params.get("kbId")),
                parseLong(params.get("ownerId")),
                parsePublished(params.get("status")),
                parseDocVisibility(params.get("visibility")),
                false,
                pageRequest(params, "updatedAt")
        );
        return table(page.map(this::docRow));
    }

    @GetMapping("/api/wiki/admin/document/options")
    public Map<String, Object> listDocumentOptions(@RequestParam Map<String, String> params) {
        Page<WikiDocument> page = docRepository.adminSearch(
                firstNonBlank(params.get("title"), params.get("keyword")),
                parseLong(params.get("kbId")),
                null,
                parsePublished(params.get("status")),
                null,
                false,
                pageRequest(params, "updatedAt")
        );
        Map<String, Object> result = ok();
        result.put("data", page.getContent().stream().map(this::docRow).toList());
        result.put("rows", page.getContent().stream().map(this::docRow).toList());
        return result;
    }

    @GetMapping("/api/wiki/admin/document/{docId}")
    public Map<String, Object> getDocument(@PathVariable Long docId, HttpServletRequest request) {
        DocumentResponse doc = documentService.get(docId, SecurityUtils.currentUser(), IpUtils.resolve(request), request.getHeader("User-Agent"));
        return okData(docRow(doc));
    }

    @PostMapping("/api/wiki/admin/document")
    public Map<String, Object> addDocument(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        CreateDocumentRequest create = new CreateDocumentRequest();
        create.setKbId(parseLong(stringValue(body.get("kbId"))));
        create.setParentId(normalizeParentId(parseLong(stringValue(body.get("parentId")))));
        create.setTitle(firstNonBlank(stringValue(body.get("title")), "未命名文档"));
        create.setMarkdownContent(firstNonBlank(stringValue(body.get("markdownContent")), ""));
        create.setVisibility(parseDocVisibility(stringValue(body.get("visibility"))));
        create.setPublished(Boolean.TRUE.equals(parsePublished(stringValue(body.get("status")))));
        documentService.create(create, SecurityUtils.currentUser(), IpUtils.resolve(request));
        return okMessage("操作成功");
    }

    @PutMapping("/api/wiki/admin/document")
    public Map<String, Object> updateDocument(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long docId = parseLong(firstNonBlank(stringValue(body.get("docId")), stringValue(body.get("id"))));
        UpdateDocumentRequest update = new UpdateDocumentRequest();
        update.setTitle(stringValue(body.get("title")));
        update.setMarkdownContent(stringValue(body.get("markdownContent")));
        update.setVisibility(parseDocVisibility(stringValue(body.get("visibility"))));
        update.setPublished(parsePublished(stringValue(body.get("status"))));
        update.setBaseVersion(parseInt(stringValue(body.get("baseVersion")), 0) == 0 ? null : parseInt(stringValue(body.get("baseVersion")), 0));
        update.setCommitMessage(firstNonBlank(stringValue(body.get("commitMessage")), "后台修改文档"));
        documentService.update(docId, update, SecurityUtils.currentUser(), IpUtils.resolve(request));
        return okMessage("操作成功");
    }

    @DeleteMapping("/api/wiki/admin/document/{docIds}")
    public Map<String, Object> deleteDocument(@PathVariable String docIds, HttpServletRequest request) {
        CurrentUser user = SecurityUtils.currentUser();
        String ip = IpUtils.resolve(request);
        for (String id : docIds.split(",")) {
            Long docId = parseLong(id);
            if (docId != null) {
                documentService.delete(docId, user, ip);
            }
        }
        return okMessage("删除成功");
    }

    @PutMapping("/api/wiki/admin/document/sort")
    public Map<String, Object> sortDocument() {
        return okMessage("操作成功");
    }

    @GetMapping("/api/wiki/admin/document/recycle")
    public Map<String, Object> recycleDocuments(@RequestParam Map<String, String> params) {
        Page<WikiDocument> page = docRepository.adminSearch(
                firstNonBlank(params.get("title"), params.get("keyword")),
                parseLong(params.get("kbId")),
                null,
                null,
                null,
                true,
                pageRequest(params, "updatedAt")
        );
        return okData(page.getContent().stream().map(this::docRow).toList());
    }

    @PostMapping("/api/wiki/admin/document/{docId}/restore")
    public Map<String, Object> restoreDocument(@PathVariable Long docId, HttpServletRequest request) {
        documentService.restore(docId, SecurityUtils.currentUser(), IpUtils.resolve(request));
        return okMessage("恢复成功");
    }

    @PostMapping("/api/wiki/admin/document/{docId}/lock")
    public Map<String, Object> lockDocument(@PathVariable Long docId) {
        return okData(documentService.lock(docId, SecurityUtils.currentUser()));
    }

    @DeleteMapping("/api/wiki/admin/document/{docId}/lock")
    public Map<String, Object> unlockDocument(@PathVariable Long docId) {
        documentService.unlock(docId, SecurityUtils.currentUser());
        return okMessage("操作成功");
    }

    @GetMapping("/api/wiki/admin/document/{docId}/versions")
    public Map<String, Object> documentVersions(@PathVariable Long docId) {
        return okData(documentService.versions(docId, SecurityUtils.currentUser()));
    }

    @GetMapping("/api/wiki/admin/document/{docId}/diff/{leftVersionId}/{rightVersionId}")
    public Map<String, Object> documentDiff(@PathVariable Long docId,
                                            @PathVariable Long leftVersionId,
                                            @PathVariable Long rightVersionId) {
        return okData(documentService.diffVersions(docId, leftVersionId, rightVersionId, SecurityUtils.currentUser()));
    }

    @PostMapping("/api/wiki/admin/document/{docId}/rollback/{versionId}")
    public Map<String, Object> rollbackDocument(@PathVariable Long docId,
                                                @PathVariable Long versionId,
                                                HttpServletRequest request) {
        return okData(documentService.rollback(docId, versionId, SecurityUtils.currentUser(), IpUtils.resolve(request)));
    }

    @GetMapping({"/api/wiki/admin/group/options", "/api/wiki/admin/tag/options"})
    public Map<String, Object> emptyOptions() {
        return okData(List.of());
    }

    @GetMapping("/api/wiki/admin/template/list")
    public Map<String, Object> listTemplate(@RequestParam Map<String, String> params) {
        Page<DocumentTemplate> page = templateRepository.adminSearch(
                firstNonBlank(params.get("templateName"), params.get("keyword")),
                parseLong(params.get("kbId")),
                parseDeletedFromStatus(params.get("status")),
                pageRequest(params, "updatedAt")
        );
        return table(page.map(this::templateRow));
    }

    @GetMapping("/api/wiki/admin/template/{templateId}")
    public Map<String, Object> getTemplate(@PathVariable Long templateId) {
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
        return okData(templateRow(template));
    }

    @PostMapping("/api/wiki/admin/template")
    @Transactional
    public Map<String, Object> addTemplate(@RequestBody Map<String, Object> body) {
        CurrentUser currentUser = SecurityUtils.currentUser();
        DocumentTemplate template = new DocumentTemplate();
        template.setId(idGenerator.nextId());
        template.setCreatorId(currentUser.getUserId());
        fillTemplate(template, body);
        templateRepository.save(template);
        return okMessage("操作成功");
    }

    @PutMapping("/api/wiki/admin/template")
    @Transactional
    public Map<String, Object> updateTemplate(@RequestBody Map<String, Object> body) {
        Long templateId = parseLong(firstNonBlank(stringValue(body.get("templateId")), stringValue(body.get("id"))));
        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "模板不存在"));
        fillTemplate(template, body);
        templateRepository.save(template);
        return okMessage("操作成功");
    }

    @DeleteMapping("/api/wiki/admin/template/{templateIds}")
    @Transactional
    public Map<String, Object> deleteTemplate(@PathVariable String templateIds) {
        for (String id : templateIds.split(",")) {
            Long templateId = parseLong(id);
            if (templateId != null) {
                templateRepository.findById(templateId).ifPresent(template -> {
                    template.setDeletedAt(LocalDateTime.now());
                    templateRepository.save(template);
                });
            }
        }
        return okMessage("删除成功");
    }

    private void fillTemplate(DocumentTemplate template, Map<String, Object> body) {
        String name = firstNonBlank(stringValue(body.get("templateName")), stringValue(body.get("name")));
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "模板名称不能为空");
        }
        template.setName(name);
        template.setKbId(parseLong(stringValue(body.get("kbId"))));
        template.setDescription(firstNonBlank(stringValue(body.get("remark")), stringValue(body.get("description"))));
        template.setMarkdownContent(firstNonBlank(stringValue(body.get("content")), stringValue(body.get("markdownContent"))));
        template.setCategory(firstNonBlank(stringValue(body.get("templateType")), stringValue(body.get("category"))));
        template.setIsPublic(true);
        if (template.getUseCount() == null) {
            template.setUseCount(0);
        }
        if ("disabled".equals(stringValue(body.get("status")))) {
            template.setDeletedAt(LocalDateTime.now());
        } else {
            template.setDeletedAt(null);
        }
    }

    private Map<String, Object> kbRow(KnowledgeBase kb) {
        String visibility = visibility(kb.getType());
        return row(
                "kbId", kb.getId(),
                "id", kb.getId(),
                "kbName", kb.getName(),
                "name", kb.getName(),
                "kbCode", "KB" + kb.getId(),
                "visibility", visibility,
                "type", kb.getType() == null ? null : kb.getType().name(),
                "teamId", kb.getTeamId(),
                "departmentId", kb.getTeamId(),
                "deptId", kb.getTeamId(),
                "teamName", teamName(kb.getTeamId()),
                "deptName", teamName(kb.getTeamId()),
                "status", kb.getDeletedAt() == null ? "enabled" : "disabled",
                "ownerId", kb.getOwnerId(),
                "ownerName", userName(kb.getOwnerId()),
                "docCount", countDocs(kb.getId()),
                "sortNum", 0,
                "description", kb.getDescription(),
                "remark", kb.getDescription(),
                "createTime", kb.getCreatedAt(),
                "updateTime", kb.getUpdatedAt()
        );
    }

    private Map<String, Object> templateRow(DocumentTemplate template) {
        KnowledgeBase kb = template.getKbId() == null ? null : kbRepository.findById(template.getKbId()).orElse(null);
        return row(
                "templateId", template.getId(),
                "id", template.getId(),
                "templateName", template.getName(),
                "name", template.getName(),
                "kbId", template.getKbId(),
                "kbName", kb == null ? "" : kb.getName(),
                "templateType", firstNonBlank(template.getCategory(), "doc"),
                "category", template.getCategory(),
                "content", template.getMarkdownContent(),
                "markdownContent", template.getMarkdownContent(),
                "status", template.getDeletedAt() == null ? "enabled" : "disabled",
                "sortNum", 0,
                "remark", template.getDescription(),
                "useCount", template.getUseCount(),
                "createTime", template.getCreatedAt(),
                "updateTime", template.getUpdatedAt()
        );
    }

    private Map<String, Object> docRow(WikiDocument doc) {
        KnowledgeBase kb = kbRepository.findById(doc.getKbId()).orElse(null);
        return row(
                "docId", doc.getId(),
                "id", doc.getId(),
                "kbId", doc.getKbId(),
                "kbName", kb == null ? "" : kb.getName(),
                "parentId", doc.getParentId() == null ? 0 : doc.getParentId(),
                "groupId", null,
                "groupName", "",
                "tagIds", List.of(),
                "tagNames", "",
                "title", doc.getTitle(),
                "summary", "",
                "markdownContent", doc.getMarkdownContent(),
                "htmlContent", doc.getHtmlContent(),
                "visibility", visibility(doc.getVisibility()),
                "status", Boolean.TRUE.equals(doc.getPublished()) ? "published" : "draft",
                "ownerId", doc.getOwnerId(),
                "ownerName", userName(doc.getOwnerId()),
                "viewCount", doc.getViewCount(),
                "versionNo", doc.getVersionNo(),
                "createTime", doc.getCreatedAt(),
                "updateTime", doc.getUpdatedAt()
        );
    }

    private Map<String, Object> docRow(DocumentResponse doc) {
        KnowledgeBase kb = kbRepository.findById(doc.getKbId()).orElse(null);
        return row(
                "docId", doc.getId(),
                "id", doc.getId(),
                "kbId", doc.getKbId(),
                "kbName", kb == null ? "" : kb.getName(),
                "parentId", doc.getParentId() == null ? 0 : doc.getParentId(),
                "groupId", null,
                "groupName", "",
                "tagIds", List.of(),
                "tagNames", "",
                "title", doc.getTitle(),
                "summary", "",
                "markdownContent", doc.getMarkdownContent(),
                "htmlContent", doc.getHtmlContent(),
                "visibility", visibility(doc.getVisibility()),
                "status", Boolean.TRUE.equals(doc.getPublished()) ? "published" : "draft",
                "ownerId", doc.getOwnerId(),
                "ownerName", userName(doc.getOwnerId()),
                "viewCount", doc.getViewCount(),
                "versionNo", doc.getVersionNo(),
                "updateTime", doc.getUpdatedAt()
        );
    }

    private Map<String, Object> memberRow(Long memberId, Long kbId, Long userId, String roleCode) {
        UserAccount user = userRepository.findById(userId).orElse(null);
        return row(
                "memberId", memberId == null ? userId : memberId,
                "kbId", kbId,
                "userId", userId,
                "userName", user == null ? "" : user.getUsername(),
                "nickName", user == null ? "" : firstNonBlank(user.getNickname(), user.getUsername()),
                "deptName", "",
                "roleCode", roleCode,
                "status", 0,
                "sortNum", 0,
                "remark", ""
        );
    }

    private PageRequest pageRequest(Map<String, String> params, String sortField) {
        int pageNum = Math.max(parseInt(params.get("pageNum"), 1) - 1, 0);
        int pageSize = Math.max(parseInt(params.get("pageSize"), 10), 1);
        return PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, sortField));
    }

    private Map<String, Object> table(Page<? extends Map<String, Object>> page) {
        return table(page.getContent(), page.getTotalElements());
    }

    private Map<String, Object> table(List<? extends Map<String, Object>> rows, long total) {
        Map<String, Object> result = ok();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> okData(Object data) {
        Map<String, Object> result = ok();
        result.put("data", data);
        return result;
    }

    private Map<String, Object> okMessage(String message) {
        Map<String, Object> result = ok();
        result.put("msg", message);
        return result;
    }

    private Map<String, Object> ok() {
        return row("code", 200, "msg", "操作成功");
    }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length - 1; i += 2) {
            Object value = values[i + 1];
            if (value instanceof LocalDateTime dateTime) {
                value = dateTime.format(DATETIME_FORMATTER);
            }
            map.put(String.valueOf(values[i]), value);
        }
        return map;
    }

    private String userName(Long userId) {
        return userRepository.findById(userId)
                .map(user -> firstNonBlank(user.getNickname(), user.getUsername()))
                .orElse("");
    }

    private Long resolveTeamId(KnowledgeBaseType type, Long requestedTeamId, CurrentUser user) {
        if (type != KnowledgeBaseType.DEPARTMENT) {
            return null;
        }
        Long teamId = requestedTeamId;
        if (teamId == null) {
            UserAccount current = userRepository.findById(user.getUserId()).orElse(null);
            teamId = current == null ? null : current.getDepartmentId();
        }
        if (teamId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队知识库必须选择所属团队");
        }
        Department team = departmentRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "团队不存在"));
        if (team.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队已停用");
        }
        return team.getId();
    }

    private String teamName(Long teamId) {
        if (teamId == null) {
            return "";
        }
        return departmentRepository.findById(teamId)
                .filter(team -> team.getDeletedAt() == null)
                .map(Department::getName)
                .orElse("");
    }

    private long countDocs(Long kbId) {
        return docRepository.adminSearch(null, kbId, null, null, null, false, PageRequest.of(0, 1)).getTotalElements();
    }

    private String visibility(KnowledgeBaseType type) {
        if (type == KnowledgeBaseType.DEPARTMENT) return "department";
        if (type == KnowledgeBaseType.PRIVATE) return "private";
        return "public";
    }

    private String visibility(DocVisibility visibility) {
        if (visibility == DocVisibility.TEAM) return "department";
        if (visibility == DocVisibility.PRIVATE) return "private";
        return "public";
    }

    private KnowledgeBaseType parseKbType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return switch (value.trim().toLowerCase()) {
            case "public", "company", "enabled" -> KnowledgeBaseType.COMPANY;
            case "department" -> KnowledgeBaseType.DEPARTMENT;
            case "private" -> KnowledgeBaseType.PRIVATE;
            default -> KnowledgeBaseType.from(value);
        };
    }

    private Boolean parseDeletedFromStatus(String status) {
        if ("enabled".equals(status) || "0".equals(status)) return false;
        if ("disabled".equals(status) || "1".equals(status)) return true;
        return null;
    }

    private Boolean parsePublished(String status) {
        if ("published".equals(status) || "enabled".equals(status) || "1".equals(status)) return true;
        if ("draft".equals(status) || "disabled".equals(status) || "0".equals(status)) return false;
        return null;
    }

    private DocVisibility parseDocVisibility(String value) {
        if (value == null || value.isBlank()) {
            return DocVisibility.PUBLIC;
        }
        return switch (value.trim().toLowerCase()) {
            case "department", "team" -> DocVisibility.TEAM;
            case "private" -> DocVisibility.PRIVATE;
            default -> DocVisibility.PUBLIC;
        };
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0L ? null : parentId;
    }

    private String roleCode(MemberRole role) {
        if (role == MemberRole.ADMIN) return "admin";
        if (role == MemberRole.EDITOR) return "editor";
        return "viewer";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() || "null".equals(value) || "undefined".equals(value) ? null : Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
