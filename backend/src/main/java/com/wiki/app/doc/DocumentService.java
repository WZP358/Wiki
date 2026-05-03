package com.wiki.app.doc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.doc.search.IDocumentSearchService;
import com.wiki.app.doc.dto.*;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseService;
import com.wiki.app.kb.KnowledgeBaseType;
import com.wiki.app.kb.MemberRole;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserTeamMembership;
import com.wiki.app.user.UserTeamMembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(10);
    private static final Duration SEARCH_CACHE_TTL = Duration.ofMinutes(3);
    private static final Duration HTML_CACHE_TTL = Duration.ofHours(12);
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    private final WikiDocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentDraftRepository draftRepository;
    private final DocumentViewLogRepository viewLogRepository;
    private final DocumentEditLogRepository editLogRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final UserRepository userRepository;
    private final UserTeamMembershipRepository teamMembershipRepository;
    private final DepartmentRepository departmentRepository;
    private final MarkdownService markdownService;
    private final SnowflakeIdGenerator idGenerator;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OperationLogService operationLogService;
    private final AsyncCleanupService asyncCleanupService;
    private final LocalDocStorageService localDocStorageService;
    private final IDocumentSearchService documentSearchService;

    public DocumentService(WikiDocumentRepository documentRepository,
                           DocumentVersionRepository versionRepository,
                           DocumentDraftRepository draftRepository,
                           DocumentViewLogRepository viewLogRepository,
                            DocumentEditLogRepository editLogRepository,
                            KnowledgeBaseService knowledgeBaseService,
                            KnowledgeBaseRepository knowledgeBaseRepository,
                            KnowledgeBaseMemberRepository knowledgeBaseMemberRepository,
                            UserRepository userRepository,
                            UserTeamMembershipRepository teamMembershipRepository,
                           DepartmentRepository departmentRepository,
                           MarkdownService markdownService,
                           SnowflakeIdGenerator idGenerator,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           OperationLogService operationLogService,
                           AsyncCleanupService asyncCleanupService,
                           LocalDocStorageService localDocStorageService,
                           IDocumentSearchService documentSearchService) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.draftRepository = draftRepository;
        this.viewLogRepository = viewLogRepository;
        this.editLogRepository = editLogRepository;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeBaseMemberRepository = knowledgeBaseMemberRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.departmentRepository = departmentRepository;
        this.markdownService = markdownService;
        this.idGenerator = idGenerator;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.operationLogService = operationLogService;
        this.asyncCleanupService = asyncCleanupService;
        this.localDocStorageService = localDocStorageService;
        this.documentSearchService = documentSearchService;
    }

    @Transactional
    public DocumentResponse create(CreateDocumentRequest request, CurrentUser user, String ip) {
        knowledgeBaseService.ensureKbEditor(request.getKbId(), user);

        WikiDocument doc = new WikiDocument();
        doc.setId(idGenerator.nextId());
        doc.setKbId(request.getKbId());
        doc.setParentId(request.getParentId());
        doc.setTitle(uniqueTitle(request.getKbId(), request.getTitle(), null));
        doc.setMarkdownContent(request.getMarkdownContent());
        doc.setHtmlContent(markdownService.toHtml(request.getMarkdownContent()));
        doc.setOwnerId(user.getUserId());
        doc.setVisibility(request.getVisibility() == null ? DocVisibility.PRIVATE : request.getVisibility());
        doc.setPublished(Boolean.TRUE.equals(request.getPublished()));
        doc.setVersionNo(1);
        doc.setViewCount(0L);
        documentRepository.save(doc);

        snapshot(doc, user, "Create document");
        invalidateListCache(doc.getKbId());
        markCacheChanged(doc.getKbId());
        cacheHtml(doc);
        documentSearchService.upsert(doc);

        // 记录修改日志
        logEdit(doc, user, "CREATE", null, doc.getTitle(), 0, doc.getMarkdownContent().length(), ip, "Create document");

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_DOC", "DOC", doc.getId().toString(), ip, doc.getTitle());
        return toResponse(doc);
    }

    public List<DocumentTreeNode> tree(Long kbId, CurrentUser user) {
        knowledgeBaseService.ensureKbVisible(kbId, user);
        return documentRepository.findByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(kbId)
                .stream()
                .filter(doc -> canRead(doc, user))
                .map(doc -> DocumentTreeNode.builder()
                        .id(doc.getId())
                        .parentId(doc.getParentId())
                        .title(doc.getTitle())
                        .versionNo(doc.getVersionNo())
                        .build())
                .toList();
    }

    @Transactional
    public DocumentResponse get(Long docId, CurrentUser user, String ip, String userAgent) {
        WikiDocument doc = loadActive(docId);
        ensureReadable(doc, user);

        long viewCount = doc.getViewCount() == null ? 0L : doc.getViewCount();
        doc.setViewCount(viewCount + 1);
        documentRepository.save(doc);
        redisTemplate.delete(hotKey(doc.getKbId()));
        markCacheChanged(doc.getKbId());

        // 记录查看日志
        logView(doc, user, ip, userAgent);

        String cachedHtml = redisTemplate.opsForValue().get(htmlKey(doc.getId()));
        if (cachedHtml != null) {
            doc.setHtmlContent(cachedHtml);
        } else {
            cacheHtml(doc);
        }
        return toResponse(doc);
    }

    @Transactional
    public DocumentResponse update(Long docId, UpdateDocumentRequest request, CurrentUser user, String ip) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);

        if (request.getBaseVersion() != null && !request.getBaseVersion().equals(doc.getVersionNo())) {
            throw new BusinessException(ErrorCode.DOC_CONFLICT, "Document has been updated by another user. Sync latest content before submitting");
        }

        String oldTitle = doc.getTitle();
        int oldLength = doc.getMarkdownContent().length();

        if (request.getTitle() != null) {
            doc.setTitle(uniqueTitle(doc.getKbId(), request.getTitle(), doc.getId()));
        }
        if (request.getMarkdownContent() != null) {
            doc.setMarkdownContent(request.getMarkdownContent());
            doc.setHtmlContent(markdownService.toHtml(request.getMarkdownContent()));
        }
        if (request.getVisibility() != null) {
            doc.setVisibility(request.getVisibility());
        }
        if (request.getPublished() != null) {
            doc.setPublished(request.getPublished());
        }
        doc.setVersionNo(doc.getVersionNo() + 1);

        documentRepository.save(doc);
        snapshot(doc, user, request.getCommitMessage() == null ? "Update document" : request.getCommitMessage());
        invalidateListCache(doc.getKbId());
        markCacheChanged(doc.getKbId());
        cacheHtml(doc);
        documentSearchService.upsert(doc);

        // 记录修改日志
        logEdit(doc, user, "UPDATE", oldTitle, doc.getTitle(), oldLength, doc.getMarkdownContent().length(), ip, request.getCommitMessage());

        operationLogService.record(user.getUserId(), user.getUsername(), "UPDATE_DOC", "DOC", doc.getId().toString(), ip, doc.getTitle());
        return toResponse(doc);
    }

    @Transactional
    public void delete(Long docId, CurrentUser user, String ip) {
        WikiDocument doc = loadActive(docId);
        ensureDeletable(doc, user);

        // 记录删除日志
        logEdit(doc, user, "DELETE", doc.getTitle(), null, doc.getMarkdownContent().length(), 0, ip, "Delete document");

        doc.setDeletedAt(LocalDateTime.now());
        documentRepository.save(doc);
        invalidateListCache(doc.getKbId());
        markCacheChanged(doc.getKbId());
        redisTemplate.delete(htmlKey(doc.getId()));
        redisTemplate.delete(lockKey(doc.getId()));
        documentSearchService.markDeleted(doc.getId());
        operationLogService.record(user.getUserId(), user.getUsername(), "DELETE_DOC", "DOC", doc.getId().toString(), ip, doc.getTitle());
    }

    public List<DocumentResponse> recycle(CurrentUser user) {
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only administrators can view deleted documents");
        }
        return documentRepository.findAll()
                .stream()
                .filter(doc -> doc.getDeletedAt() != null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponse restore(Long docId, CurrentUser user, String ip) {
        WikiDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Document not found"));
        if (doc.getDeletedAt() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Document is not in recycle bin");
        }
        if (!user.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only administrators can restore document");
        }
        doc.setDeletedAt(null);
        documentRepository.save(doc);
        invalidateListCache(doc.getKbId());
        markCacheChanged(doc.getKbId());
        documentSearchService.upsert(doc);
        operationLogService.record(user.getUserId(), user.getUsername(), "RESTORE_DOC", "DOC", doc.getId().toString(), ip, doc.getTitle());
        return toResponse(doc);
    }

    @Transactional
    public void purge(Long docId, CurrentUser user, boolean confirmed, String ip) {
        throw new BusinessException(ErrorCode.FORBIDDEN, "Documents are soft-deleted only and cannot be purged");
    }

    public List<DocumentVersionResponse> versions(Long docId, CurrentUser user) {
        WikiDocument doc = loadActive(docId);
        ensureReadable(doc, user);
        return versionRepository.findByDocIdOrderByVersionNoDesc(docId)
                .stream()
                .map(v -> DocumentVersionResponse.builder()
                        .id(v.getId())
                        .versionNo(v.getVersionNo())
                        .title(v.getTitle())
                        .markdownContent(v.getMarkdownContent())
                        .htmlContent(v.getHtmlContent())
                        .editorId(v.getEditorId())
                        .editorName(v.getEditorName())
                        .commitMessage(v.getCommitMessage())
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();
    }

    public VersionDiffResponse diffVersions(Long docId, Long leftVersionId, Long rightVersionId, CurrentUser user) {
        WikiDocument doc = loadActive(docId);
        ensureReadable(doc, user);

        DocumentVersion left = versionRepository.findById(leftVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Left version not found"));
        DocumentVersion right = versionRepository.findById(rightVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Right version not found"));

        if (!left.getDocId().equals(docId) || !right.getDocId().equals(docId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Version does not belong to this document");
        }

        List<DiffLineResponse> lines = diffByLine(left.getMarkdownContent(), right.getMarkdownContent());
        return VersionDiffResponse.builder()
                .docId(docId)
                .leftVersionId(leftVersionId)
                .rightVersionId(rightVersionId)
                .leftVersionNo(left.getVersionNo())
                .rightVersionNo(right.getVersionNo())
                .lines(lines)
                .build();
    }

    @Transactional
    public DocumentResponse rollback(Long docId, Long versionId, CurrentUser user, String ip) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);

        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "History version not found"));
        if (!version.getDocId().equals(docId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Version does not belong to this document");
        }

        doc.setTitle(uniqueTitle(doc.getKbId(), version.getTitle(), doc.getId()));
        doc.setMarkdownContent(version.getMarkdownContent());
        doc.setHtmlContent(version.getHtmlContent());
        doc.setVersionNo(doc.getVersionNo() + 1);
        documentRepository.save(doc);

        snapshot(doc, user, "Rollback to v" + version.getVersionNo());
        invalidateListCache(doc.getKbId());
        markCacheChanged(doc.getKbId());
        cacheHtml(doc);
        documentSearchService.upsert(doc);
        operationLogService.record(user.getUserId(), user.getUsername(), "ROLLBACK_DOC", "DOC", doc.getId().toString(), ip, "Rollback to version " + version.getVersionNo());
        return toResponse(doc);
    }

    public List<DocumentResponse> search(Long kbId, String keyword, CurrentUser user) {
        knowledgeBaseService.ensureKbVisible(kbId, user);
        return searchDocsWithCache(kbId, keyword)
                .stream()
                .filter(doc -> canRead(doc, user))
                .map(doc -> toSearchResponse(doc, keyword))
                .toList();
    }

    public List<DocumentResponse> latest(Long kbId, CurrentUser user) {
        knowledgeBaseService.ensureKbVisible(kbId, user);
        return listWithCache(kbId, latestKey(kbId), () -> documentRepository.findTop10ByKbIdAndDeletedAtIsNullOrderByUpdatedAtDesc(kbId)
                .stream()
                .limit(5)
                .toList(), user);
    }

    public List<DocumentResponse> hot(Long kbId, CurrentUser user) {
        knowledgeBaseService.ensureKbVisible(kbId, user);
        return listWithCache(kbId, hotKey(kbId), () -> documentRepository.findTop10ByKbIdAndDeletedAtIsNullOrderByViewCountDesc(kbId)
                .stream()
                .limit(5)
                .toList(), user);
    }

    public EditLockResponse lock(Long docId, CurrentUser user) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);
        String key = lockKey(docId);
        String lockOwner = redisTemplate.opsForValue().get(key);
        if (lockOwner != null) {
            if (lockOwner.equals(user.getUsername())) {
                redisTemplate.expire(key, LOCK_TTL);
                return new EditLockResponse(true, user.getUsername(), "\u7f16\u8f91\u9501\u5df2\u7eed\u671f");
            }
            return new EditLockResponse(false, lockOwner, "\u6587\u6863\u6b63\u5728\u7531 " + lockOwner + " \u7f16\u8f91");
        }
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, user.getUsername(), LOCK_TTL);
        if (Boolean.FALSE.equals(locked) && lockOwner == null) {
            lockOwner = redisTemplate.opsForValue().get(key);
            if (user.getUsername().equals(lockOwner)) {
                redisTemplate.expire(key, LOCK_TTL);
                return new EditLockResponse(true, user.getUsername(), "\u7f16\u8f91\u9501\u5df2\u7eed\u671f");
            }
            return new EditLockResponse(false, lockOwner, "\u6587\u6863\u6b63\u5728\u7531 " + (lockOwner == null ? "\u5176\u4ed6\u7528\u6237" : lockOwner) + " \u7f16\u8f91");
        }
        return new EditLockResponse(true, user.getUsername(), "\u5df2\u83b7\u5f97\u7f16\u8f91\u9501");
    }

    public void unlock(Long docId, CurrentUser user) {
        String key = lockKey(docId);
        String lockOwner = redisTemplate.opsForValue().get(key);
        if (lockOwner != null && lockOwner.equals(user.getUsername())) {
            redisTemplate.delete(key);
        }
    }

    @Transactional
    public DraftResponse saveDraft(Long docId, SaveDraftRequest request, CurrentUser user) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);
        DocumentDraft draft = draftRepository.findByDocIdAndUserId(docId, user.getUserId())
                .orElseGet(() -> {
                    DocumentDraft created = new DocumentDraft();
                    created.setId(idGenerator.nextId());
                    created.setDocId(docId);
                    created.setUserId(user.getUserId());
                    return created;
                });
        draft.setTitle(request.getTitle());
        draft.setMarkdownContent(request.getMarkdownContent());
        draftRepository.save(draft);
        return toDraftResponse(draft);
    }

    public DraftResponse draft(Long docId, CurrentUser user) {
        return draftRepository.findByDocIdAndUserId(docId, user.getUserId())
                .map(this::toDraftResponse)
                .orElse(null);
    }

    public RealtimeDocSnapshot realtimeSnapshot(Long docId, CurrentUser user) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);
        return RealtimeDocSnapshot.builder()
                .docId(doc.getId())
                .title(doc.getTitle())
                .markdownContent(doc.getMarkdownContent())
                .versionNo(doc.getVersionNo())
                .ownerId(doc.getOwnerId())
                .build();
    }

    @Transactional
    public RealtimeDocSnapshot applyRealtimeUpdate(Long docId,
                                                   String title,
                                                   String markdownContent,
                                                   CurrentUser user,
                                                   String ip,
                                                   String commitMessage) {
        WikiDocument doc = loadActive(docId);
        ensureEditable(doc, user);
        if (title != null) {
            doc.setTitle(uniqueTitle(doc.getKbId(), title, doc.getId()));
        }
        if (markdownContent != null) {
            doc.setMarkdownContent(markdownContent);
            doc.setHtmlContent(markdownService.toHtml(markdownContent));
        }
        doc.setVersionNo(doc.getVersionNo() + 1);
        documentRepository.save(doc);
        snapshot(doc, user, commitMessage == null ? "Realtime collaborative update" : commitMessage);
        invalidateListCache(doc.getKbId());
        cacheHtml(doc);
        documentSearchService.upsert(doc);
        operationLogService.record(user.getUserId(), user.getUsername(), "COLLAB_EDIT", "DOC", doc.getId().toString(), ip, doc.getTitle());
        return RealtimeDocSnapshot.builder()
                .docId(doc.getId())
                .title(doc.getTitle())
                .markdownContent(doc.getMarkdownContent())
                .versionNo(doc.getVersionNo())
                .ownerId(doc.getOwnerId())
                .build();
    }

    public WikiDocument loadActive(Long docId) {
        WikiDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Document not found"));
        if (doc.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Document has been deleted");
        }
        return doc;
    }

    private void ensureReadable(WikiDocument doc, CurrentUser user) {
        knowledgeBaseService.ensureKbVisible(doc.getKbId(), user);
        if (!canRead(doc, user)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你可以进入该知识库，但该文档对你不可见。请联系文档作者或知识库管理员调整文档可见性。");
        }
    }

    private boolean canRead(WikiDocument doc, CurrentUser user) {
        if (doc.getOwnerId().equals(user.getUserId())) {
            return true;
        }
        KnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId()).orElse(null);
        if (kb != null) {
            if (kb.getOwnerId().equals(user.getUserId())) {
                return true;
            }
            if (knowledgeBaseMemberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), user.getUserId()).isPresent()) {
                return doc.getVisibility() != DocVisibility.PRIVATE;
            }
        }
        if (doc.getVisibility() == DocVisibility.PUBLIC) {
            return true;
        }
        if (doc.getVisibility() == DocVisibility.PRIVATE) {
            return false;
        }

        return kb != null
                && doc.getVisibility() == DocVisibility.TEAM
                && isSameTeam(user.getUserId(), kb);
    }

    private void ensureEditable(WikiDocument doc, CurrentUser user) {
        if (doc.getVisibility() == DocVisibility.PRIVATE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "私有文档只能由作者维护。");
        }

        KnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
        if (doc.getVisibility() == DocVisibility.PUBLIC && canEditInKb(kb, user)) {
            return;
        }
        if (doc.getVisibility() == DocVisibility.TEAM
                && isSameTeam(user.getUserId(), kb)
                && canEditInKb(kb, user)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "你可以查看该知识库，但没有编辑权限；请联系知识库管理员加入协作名单。");
    }

    private boolean canEditInKb(KnowledgeBase kb, CurrentUser user) {
        return knowledgeBaseMemberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), user.getUserId())
                .map(member -> member.getRole() == MemberRole.EDITOR || member.getRole() == MemberRole.ADMIN)
                .orElse(false);
    }

    private void ensureDeletable(WikiDocument doc, CurrentUser user) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(doc.getKbId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
        boolean kbManager = knowledgeBaseMemberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), user.getUserId())
                .map(member -> member.getRole() == MemberRole.ADMIN)
                .orElse(false);
        if (!kbManager) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Only knowledge base managers can delete documents");
        }
    }

    private boolean canManageInKb(KnowledgeBase kb, CurrentUser user) {
        return knowledgeBaseMemberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), user.getUserId())
                .map(member -> member.getRole() == MemberRole.ADMIN)
                .orElse(false);
    }

    private boolean isSameTeam(Long userId, KnowledgeBase kb) {
        UserAccount currentUser = userRepository.findById(userId).orElse(null);
        return currentUser != null
                && userBelongsToTeam(userId, effectiveTeamId(kb));
    }

    private boolean userBelongsToTeam(Long userId, Long teamId) {
        if (teamId == null) {
            return false;
        }
        UserAccount currentUser = userRepository.findById(userId).orElse(null);
        if (currentUser != null && isSameOrChildTeam(currentUser.getDepartmentId(), teamId)) {
            return true;
        }
        return teamMembershipRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(UserTeamMembership::getTeamId)
                .anyMatch(userTeamId -> isSameOrChildTeam(userTeamId, teamId));
    }

    private boolean isSameOrChildTeam(Long userTeamId, Long kbTeamId) {
        if (userTeamId == null || kbTeamId == null) {
            return false;
        }
        Long currentId = userTeamId;
        for (int depth = 0; currentId != null && depth < 32; depth++) {
            if (kbTeamId.equals(currentId)) {
                return true;
            }
            currentId = departmentRepository.findById(currentId)
                    .map(Department::getParentId)
                    .orElse(null);
        }
        return false;
    }

    private Long effectiveTeamId(KnowledgeBase kb) {
        if (kb.getTeamId() != null) {
            return kb.getTeamId();
        }
        UserAccount owner = userRepository.findById(kb.getOwnerId()).orElse(null);
        return owner == null ? null : owner.getDepartmentId();
    }

    private void snapshot(WikiDocument doc, CurrentUser user, String message) {
        DocumentVersion version = new DocumentVersion();
        version.setId(idGenerator.nextId());
        version.setDocId(doc.getId());
        version.setVersionNo(doc.getVersionNo());
        version.setTitle(doc.getTitle());
        version.setMarkdownContent(doc.getMarkdownContent());
        version.setHtmlContent(doc.getHtmlContent());
        version.setEditorId(user.getUserId());
        version.setEditorName(user.getUsername());
        version.setCommitMessage(message);
        versionRepository.save(version);
    }

    private String uniqueTitle(Long kbId, String requestedTitle, Long currentDocId) {
        String baseTitle = normalizeTitle(requestedTitle);
        List<WikiDocument> existingDocs = documentRepository.findByKbIdAndTitleStartingWithAndDeletedAtIsNull(kbId, baseTitle);
        Set<String> usedTitles = existingDocs.stream()
                .filter(doc -> currentDocId == null || !doc.getId().equals(currentDocId))
                .map(WikiDocument::getTitle)
                .collect(Collectors.toCollection(HashSet::new));

        if (!usedTitles.contains(baseTitle)) {
            return baseTitle;
        }

        String suffixBase = stripAutoSuffix(baseTitle);
        List<WikiDocument> suffixBaseDocs = suffixBase.equals(baseTitle)
                ? existingDocs
                : documentRepository.findByKbIdAndTitleStartingWithAndDeletedAtIsNull(kbId, suffixBase);
        usedTitles.addAll(suffixBaseDocs.stream()
                .filter(doc -> currentDocId == null || !doc.getId().equals(currentDocId))
                .map(WikiDocument::getTitle)
                .toList());

        int suffix = 1;
        String candidate;
        do {
            candidate = suffixBase + "（" + suffix + "）";
            suffix++;
        } while (usedTitles.contains(candidate));
        return candidate;
    }

    private String normalizeTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            normalized = "未命名文档";
        }
        return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
    }

    private String stripAutoSuffix(String title) {
        return title.replaceFirst("（\\d+）$", "");
    }

    private List<DocumentResponse> listWithCache(Long kbId, String key, DocListSupplier supplier, CurrentUser user) {
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                List<Long> ids = objectMapper.readValue(cached, new TypeReference<>() {
                });
                List<WikiDocument> docs = documentRepository.findByIdInAndDeletedAtIsNull(ids);
                Map<Long, WikiDocument> byId = docs.stream().collect(Collectors.toMap(WikiDocument::getId, doc -> doc));
                return ids.stream()
                        .map(byId::get)
                        .filter(Objects::nonNull)
                        .filter(doc -> canRead(doc, user))
                        .map(this::toResponse)
                        .toList();
            } catch (Exception ignored) {
            }
        }

        List<WikiDocument> docs = supplier.get();
        try {
            List<Long> ids = docs.stream().map(WikiDocument::getId).toList();
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(ids), LIST_CACHE_TTL);
        } catch (Exception ignored) {
        }
        return docs.stream()
                .filter(doc -> canRead(doc, user))
                .map(this::toResponse)
                .toList();
    }

    private void invalidateListCache(Long kbId) {
        redisTemplate.delete(latestKey(kbId));
        redisTemplate.delete(hotKey(kbId));
        redisTemplate.delete(searchKey(kbId));
    }

    private void markCacheChanged(Long kbId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(cacheVersionKey(kbId), timestamp, Duration.ofDays(1));
        redisTemplate.convertAndSend("wiki:doc-cache-events", kbId + ":" + timestamp);
    }

    private void cacheHtml(WikiDocument doc) {
        if (!Boolean.TRUE.equals(doc.getPublished())) {
            redisTemplate.delete(htmlKey(doc.getId()));
            localDocStorageService.deleteDocArtifacts(doc.getId());
            return;
        }
        redisTemplate.opsForValue().set(htmlKey(doc.getId()), doc.getHtmlContent(), HTML_CACHE_TTL);
        localDocStorageService.savePublishedHtml(doc.getId(), doc.getHtmlContent());
    }

    private DocumentResponse toResponse(WikiDocument doc) {
        String html = doc.getHtmlContent();
        if (Boolean.TRUE.equals(doc.getPublished())) {
            String cachedHtml = redisTemplate.opsForValue().get(htmlKey(doc.getId()));
            if (cachedHtml != null) {
                html = cachedHtml;
            }
        }
        UserAccount owner = userRepository.findById(doc.getOwnerId()).orElse(null);
        return DocumentResponse.builder()
                .id(doc.getId())
                .kbId(doc.getKbId())
                .parentId(doc.getParentId())
                .title(doc.getTitle())
                .markdownContent(doc.getMarkdownContent())
                .htmlContent(html)
                .ownerId(doc.getOwnerId())
                .ownerUsername(owner == null ? null : owner.getUsername())
                .ownerName(owner == null ? null : firstNonBlank(owner.getNickname(), owner.getUsername()))
                .visibility(doc.getVisibility())
                .viewCount(doc.getViewCount())
                .versionNo(doc.getVersionNo())
                .published(doc.getPublished())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }

    private DocumentResponse toSearchResponse(WikiDocument doc, String keyword) {
        DocumentResponse response = toResponse(doc);
        response.setSearchHighlight(buildSearchHighlight(doc, keyword));
        return response;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String buildSearchHighlight(WikiDocument doc, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String term = keyword.trim().toLowerCase();
        String source = (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(term))
                ? doc.getTitle()
                : doc.getMarkdownContent();
        if (source == null || source.isBlank()) {
            return null;
        }
        String lower = source.toLowerCase();
        int index = lower.indexOf(term);
        if (index < 0) {
            return source.length() <= 120 ? source : source.substring(0, 120) + "...";
        }
        int start = Math.max(0, index - 40);
        int end = Math.min(source.length(), index + keyword.trim().length() + 80);
        String snippet = source.substring(start, end);
        return (start > 0 ? "..." : "") + snippet + (end < source.length() ? "..." : "");
    }

    private DraftResponse toDraftResponse(DocumentDraft draft) {
        return DraftResponse.builder()
                .docId(draft.getDocId())
                .userId(draft.getUserId())
                .title(draft.getTitle())
                .markdownContent(draft.getMarkdownContent())
                .updatedAt(draft.getUpdatedAt())
                .build();
    }

    private String latestKey(Long kbId) {
        return "docs:latest:" + kbId;
    }

    private String hotKey(Long kbId) {
        return "docs:hot:" + kbId;
    }

    private String searchKey(Long kbId) {
        return "docs:search:" + kbId;
    }

    private String htmlKey(Long docId) {
        return "doc:html:" + docId;
    }

    private String lockKey(Long docId) {
        return "editing:" + docId;
    }

    private String cacheVersionKey(Long kbId) {
        return "docs:cache-version:" + kbId;
    }

    public String generateShareToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public void onDocPurgedByScheduler(Long docId) {
        documentSearchService.delete(docId);
    }

    @Transactional
    public DocumentResponse publicView(Long docId) {
        WikiDocument doc = loadActive(docId);
        if (!Boolean.TRUE.equals(doc.getPublished())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Document is not published and cannot be viewed publicly");
        }
        doc.setViewCount(doc.getViewCount() + 1);
        documentRepository.save(doc);
        redisTemplate.delete(hotKey(doc.getKbId()));
        markCacheChanged(doc.getKbId());
        String cachedHtml = redisTemplate.opsForValue().get(htmlKey(doc.getId()));
        if (cachedHtml != null) {
            doc.setHtmlContent(cachedHtml);
        } else {
            cacheHtml(doc);
        }
        return toResponse(doc);
    }

    @FunctionalInterface
    private interface DocListSupplier {
        List<WikiDocument> get();
    }

    private List<WikiDocument> searchDocs(Long kbId, String keyword) {
        List<Long> indexedDocIds = documentSearchService.searchDocIds(kbId, keyword);
        if (indexedDocIds == null) {
            return documentRepository.search(kbId, keyword);
        }
        if (indexedDocIds.isEmpty()) {
            return List.of();
        }
        List<WikiDocument> docs = documentRepository.findByIdInAndDeletedAtIsNull(indexedDocIds);
        Map<Long, WikiDocument> docMap = docs.stream().collect(Collectors.toMap(WikiDocument::getId, doc -> doc));
        return indexedDocIds.stream()
                .map(docMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<WikiDocument> searchDocsWithCache(Long kbId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return searchDocs(kbId, keyword);
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        String key = searchKey(kbId);
        String hashKey = normalizedKeyword.length() > 64
                ? Integer.toHexString(normalizedKeyword.hashCode())
                : normalizedKeyword;
        String cached = redisTemplate.opsForHash().get(key, hashKey) instanceof String value ? value : null;
        if (cached != null) {
            try {
                List<Long> ids = objectMapper.readValue(cached, new TypeReference<>() {
                });
                List<WikiDocument> docs = documentRepository.findByIdInAndDeletedAtIsNull(ids);
                Map<Long, WikiDocument> docMap = docs.stream().collect(Collectors.toMap(WikiDocument::getId, doc -> doc));
                return ids.stream()
                        .map(docMap::get)
                        .filter(Objects::nonNull)
                        .toList();
            } catch (Exception ignored) {
            }
        }
        List<WikiDocument> docs = searchDocs(kbId, keyword);
        try {
            redisTemplate.opsForHash().put(key, hashKey, objectMapper.writeValueAsString(docs.stream().map(WikiDocument::getId).toList()));
            redisTemplate.expire(key, SEARCH_CACHE_TTL);
        } catch (Exception ignored) {
        }
        return docs;
    }

    private List<DiffLineResponse> diffByLine(String leftText, String rightText) {
        String[] left = (leftText == null ? "" : leftText).split("\\r?\\n", -1);
        String[] right = (rightText == null ? "" : rightText).split("\\r?\\n", -1);

        int m = left.length;
        int n = right.length;
        int[][] lcs = new int[m + 1][n + 1];
        for (int i = m - 1; i >= 0; i--) {
            for (int j = n - 1; j >= 0; j--) {
                if (left[i].equals(right[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        List<DiffLineResponse> result = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < m && j < n) {
            if (left[i].equals(right[j])) {
                result.add(DiffLineResponse.builder().type("UNCHANGED").left(left[i]).right(right[j]).build());
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                result.add(DiffLineResponse.builder().type("REMOVED").left(left[i]).right("").build());
                i++;
            } else {
                result.add(DiffLineResponse.builder().type("ADDED").left("").right(right[j]).build());
                j++;
            }
        }
        while (i < m) {
            result.add(DiffLineResponse.builder().type("REMOVED").left(left[i]).right("").build());
            i++;
        }
        while (j < n) {
            result.add(DiffLineResponse.builder().type("ADDED").left("").right(right[j]).build());
            j++;
        }

        List<DiffLineResponse> normalized = new ArrayList<>();
        for (int idx = 0; idx < result.size(); idx++) {
            DiffLineResponse line = result.get(idx);
            if ("REMOVED".equals(line.getType()) && idx + 1 < result.size() && "ADDED".equals(result.get(idx + 1).getType())) {
                DiffLineResponse next = result.get(idx + 1);
                normalized.add(DiffLineResponse.builder().type("CHANGED").left(line.getLeft()).right(next.getRight()).build());
                idx++;
            } else {
                normalized.add(line);
            }
        }
        return normalized;
    }

    private void logView(WikiDocument doc, CurrentUser user, String ip, String userAgent) {
        DocumentViewLog log = new DocumentViewLog();
        log.setId(idGenerator.nextId());
        log.setDocId(doc.getId());
        log.setUserId(user.getUserId());
        log.setUsername(user.getUsername());
        log.setIp(ip);
        log.setUserAgent(userAgent);
        log.setCreatedAt(LocalDateTime.now());
        viewLogRepository.save(log);
    }

    private void logEdit(WikiDocument doc, CurrentUser user, String action, String titleBefore, String titleAfter,
                         int contentLengthBefore, int contentLengthAfter, String ip, String commitMessage) {
        DocumentEditLog log = new DocumentEditLog();
        log.setId(idGenerator.nextId());
        log.setDocId(doc.getId());
        log.setUserId(user.getUserId());
        log.setUsername(user.getUsername());
        log.setAction(action);
        log.setTitleBefore(titleBefore);
        log.setTitleAfter(titleAfter);
        log.setContentLengthBefore(contentLengthBefore);
        log.setContentLengthAfter(contentLengthAfter);
        log.setIp(ip);
        log.setCommitMessage(commitMessage);
        log.setCreatedAt(LocalDateTime.now());
        editLogRepository.save(log);
    }
}
