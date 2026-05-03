package com.wiki.app.kb;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.dto.CreateKnowledgeBaseRequest;
import com.wiki.app.kb.dto.InviteMemberRequest;
import com.wiki.app.kb.dto.KbMemberDetailResponse;
import com.wiki.app.kb.dto.KnowledgeBaseResponse;
import com.wiki.app.kb.dto.MemberResponse;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembership;
import com.wiki.app.user.UserTeamMembershipRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KnowledgeBaseService {
    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UserTeamMembershipRepository teamMembershipRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final OperationLogService operationLogService;
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository,
                                KnowledgeBaseMemberRepository memberRepository,
                                DepartmentRepository departmentRepository,
                                UserRepository userRepository,
                                UserTeamMembershipRepository teamMembershipRepository,
                                SnowflakeIdGenerator idGenerator,
                                OperationLogService operationLogService,
                                KnowledgeBaseMemberSyncService memberSyncService) {
        this.kbRepository = kbRepository;
        this.memberRepository = memberRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.idGenerator = idGenerator;
        this.operationLogService = operationLogService;
        this.memberSyncService = memberSyncService;
    }

    @Transactional
    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request, CurrentUser user, String ip) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(idGenerator.nextId());
        kb.setName(request.getName());
        kb.setType(request.getType());
        kb.setDescription(request.getDescription());
        kb.setOwnerId(user.getUserId());
        kb.setParentId(resolveParentIdForCreate(request.getParentId(), user));
        kb.setTeamId(resolveTeamId(request.getType(), request.getTeamId(), user));
        kbRepository.save(kb);

        KnowledgeBaseMember ownerMember = new KnowledgeBaseMember();
        ownerMember.setId(idGenerator.nextId());
        ownerMember.setKbId(kb.getId());
        ownerMember.setUserId(user.getUserId());
        ownerMember.setRole(MemberRole.ADMIN);
        memberRepository.save(ownerMember);
        memberSyncService.syncKnowledgeBaseAutoReaders(kb);

        operationLogService.record(user.getUserId(), user.getUsername(), "CREATE_KB", "KB", kb.getId().toString(), ip, "Create knowledge base");
        return toResponse(kb, MemberRole.ADMIN.name());
    }

    @Transactional
    public KnowledgeBaseResponse update(Long kbId, CreateKnowledgeBaseRequest request, CurrentUser user, String ip) {
        KnowledgeBase kb = loadActiveKb(kbId);
        ensureKbAdmin(kbId, user);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setType(request.getType());
        kb.setParentId(resolveParentIdForUpdate(kbId, request.getParentId(), user));
        kb.setTeamId(resolveTeamId(request.getType(), request.getTeamId(), user));
        kbRepository.save(kb);
        memberSyncService.syncKnowledgeBaseAutoReaders(kb);
        operationLogService.record(user.getUserId(), user.getUsername(), "UPDATE_KB", "KB", kb.getId().toString(), ip, "Update knowledge base");
        return toResponse(kb, roleFor(kb, user));
    }

    @Transactional
    public void delete(Long kbId, CurrentUser user, String ip) {
        KnowledgeBase kb = loadActiveKb(kbId);
        ensureKbAdmin(kbId, user);
        kb.setDeletedAt(LocalDateTime.now());
        kbRepository.save(kb);
        operationLogService.record(user.getUserId(), user.getUsername(), "DELETE_KB", "KB", kb.getId().toString(), ip, "Disable knowledge base");
    }

    public List<KnowledgeBaseResponse> listMine(CurrentUser user) {
        Map<Long, KnowledgeBaseResponse> responses = new LinkedHashMap<>();
        UserAccount userAccount = userRepository.findById(user.getUserId()).orElse(null);

        for (KnowledgeBaseMember member : memberRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())) {
            KnowledgeBase kb = kbRepository.findById(member.getKbId()).orElse(null);
            if (kb != null && kb.getDeletedAt() == null) {
                responses.put(kb.getId(), toResponse(kb, member.getRole().name()));
            }
        }

        for (KnowledgeBase kb : kbRepository.findByOwnerIdAndDeletedAtIsNull(user.getUserId())) {
            responses.putIfAbsent(kb.getId(), toResponse(kb, ensureOwnerMember(kb).name()));
        }

        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.COMPANY)) {
            responses.putIfAbsent(kb.getId(), toResponse(kb, MemberRole.READER.name()));
        }

        if (userAccount != null && userAccount.getDepartmentId() != null) {
            for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.DEPARTMENT)) {
                if (userBelongsToTeam(user.getUserId(), effectiveTeamId(kb))) {
                    responses.putIfAbsent(kb.getId(), toResponse(kb, MemberRole.READER.name()));
                }
            }
        }

        return new ArrayList<>(responses.values());
    }

    public List<KnowledgeBaseResponse> listPublicByUser(Long userId) {
        return kbRepository.findByOwnerIdAndTypeAndDeletedAtIsNull(userId, KnowledgeBaseType.COMPANY)
                .stream()
                .map(kb -> toResponse(kb, MemberRole.READER.name()))
                .toList();
    }

    public KnowledgeBaseResponse getForCurrent(Long kbId, CurrentUser currentUser) {
        KnowledgeBase kb = loadActiveKb(kbId);
        ensureKbVisible(kbId, currentUser);
        return toResponse(kb, roleFor(kb, currentUser));
    }

    public List<KnowledgeBaseResponse> listChildren(Long kbId, CurrentUser currentUser) {
        ensureKbVisible(kbId, currentUser);
        List<KnowledgeBaseResponse> result = new ArrayList<>();
        for (KnowledgeBase child : kbRepository.findByParentIdAndDeletedAtIsNull(kbId)) {
            try {
                ensureKbVisible(child.getId(), currentUser);
                result.add(toResponse(child, roleFor(child, currentUser)));
            } catch (BusinessException ignored) {
                // Child knowledge bases keep independent visibility and member roles.
            }
        }
        return result;
    }

    public List<KnowledgeBaseResponse> searchVisibleKbs(String keyword, CurrentUser currentUser) {
        List<KnowledgeBaseResponse> visible = new ArrayList<>();
        for (KnowledgeBase kb : kbRepository.searchByKeyword(keyword)) {
            try {
                ensureKbVisible(kb.getId(), currentUser);
                visible.add(toResponse(kb, roleFor(kb, currentUser)));
            } catch (BusinessException ignored) {
                // Skip invisible knowledge bases.
            }
        }
        return visible;
    }

    public List<KnowledgeBaseResponse> listByDepartment(Long departmentId, CurrentUser currentUser) {
        List<KnowledgeBaseResponse> result = new ArrayList<>();
        for (KnowledgeBase kb : kbRepository.findByTypeAndDeletedAtIsNull(KnowledgeBaseType.DEPARTMENT)) {
            if (!departmentId.equals(effectiveTeamId(kb))) {
                continue;
            }
            try {
                ensureKbVisible(kb.getId(), currentUser);
                result.add(toResponse(kb, roleFor(kb, currentUser)));
            } catch (BusinessException ignored) {
                // Skip invisible knowledge bases.
            }
        }
        return result;
    }

    public void ensureKbVisible(Long kbId, CurrentUser user) {
        KnowledgeBase kb = loadActiveKb(kbId);
        if (user.isAdmin()) {
            return;
        }
        if (memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, user.getUserId()).isPresent()) {
            return;
        }
        if (kb.getType() == KnowledgeBaseType.COMPANY) {
            return;
        }
        if (kb.getType() == KnowledgeBaseType.DEPARTMENT && isSameTeam(user.getUserId(), kb)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "你可以看到公开知识库、同团队知识库或被邀请的知识库；如需访问请联系系统管理员分配团队，或联系知识库管理员邀请你加入。");
    }

    public void ensureKbEditor(Long kbId, CurrentUser user) {
        loadActiveKb(kbId);
        MemberRole role = memberRole(kbId, user.getUserId()).orElse(null);
        if (role == MemberRole.ADMIN || role == MemberRole.EDITOR) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "你当前只有查看权限。请联系知识库管理员加入协作名单，并授予编辑或管理权限。");
    }

    public void ensureKbAdmin(Long kbId, CurrentUser user) {
        loadActiveKb(kbId);
        if (memberRole(kbId, user.getUserId()).filter(role -> role == MemberRole.ADMIN).isPresent()) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "只有知识库管理员可以执行该操作。");
    }

    public List<KbMemberDetailResponse> listMembers(Long kbId, CurrentUser currentUser) {
        ensureKbVisible(kbId, currentUser);
        KnowledgeBase kb = loadActiveKb(kbId);
        ensureOwnerMember(kb);

        Map<Long, KbMemberDetailResponse> resultByUser = new LinkedHashMap<>();
        for (KnowledgeBaseMember member : memberRepository.findByKbIdAndDeletedAtIsNull(kbId)) {
            UserAccount user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null || user.getRole() != UserRole.USER) {
                continue;
            }
            resultByUser.put(user.getId(), toMemberDetail(user, member.getRole()));
        }
        return new ArrayList<>(resultByUser.values());
    }

    @Transactional
    public MemberResponse upsertMember(Long kbId, InviteMemberRequest request, CurrentUser currentUser, String ip) {
        ensureKbAdmin(kbId, currentUser);
        KnowledgeBase kb = loadActiveKb(kbId);
        ensureOwnerMember(kb);

        if (request.getRole() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Member role is required");
        }

        Long userId = resolveInviteUserId(request);
        UserAccount invited = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        if (invited.getRole() != UserRole.USER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Only normal users can be invited into a knowledge base");
        }

        Optional<KnowledgeBaseMember> existing = memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, userId);
        if (existing.isPresent() && existing.get().getRole() == MemberRole.ADMIN && request.getRole() != MemberRole.ADMIN) {
            ensureCanRemoveAdmin(kbId, userId);
        }

        KnowledgeBaseMember member = existing.orElseGet(() -> {
            KnowledgeBaseMember created = new KnowledgeBaseMember();
            created.setId(idGenerator.nextId());
            created.setKbId(kbId);
            created.setUserId(userId);
            return created;
        });
        member.setRole(request.getRole());
        memberRepository.save(member);

        operationLogService.record(currentUser.getUserId(), currentUser.getUsername(),
                existing.isPresent() ? "UPDATE_KB_MEMBER" : "INVITE_KB_MEMBER",
                "KB_MEMBER", kbId + ":" + userId, ip,
                "Member changed: " + invited.getUsername() + " -> " + request.getRole());

        return MemberResponse.builder()
                .userId(userId)
                .role(request.getRole())
                .build();
    }

    private KnowledgeBase loadActiveKb(Long kbId) {
        KnowledgeBase kb = kbRepository.findById(kbId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
        if (kb.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found");
        }
        return kb;
    }

    private Long resolveParentIdForCreate(Long parentId, CurrentUser user) {
        if (parentId == null) {
            return null;
        }
        KnowledgeBase parent = loadActiveKb(parentId);
        ensureKbAdmin(parent.getId(), user);
        return parent.getId();
    }

    private Long resolveParentIdForUpdate(Long kbId, Long parentId, CurrentUser user) {
        if (parentId == null) {
            return null;
        }
        if (kbId.equals(parentId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Knowledge base parent cannot be itself");
        }
        KnowledgeBase parent = loadActiveKb(parentId);
        ensureKbAdmin(parent.getId(), user);
        return parent.getId();
    }

    private Long resolveTeamId(KnowledgeBaseType type, Long requestedTeamId, CurrentUser user) {
        if (type != KnowledgeBaseType.DEPARTMENT) {
            return null;
        }

        Long teamId = requestedTeamId;
        if (teamId == null) {
            UserAccount current = userRepository.findById(user.getUserId()).orElse(null);
            teamId = current == null ? null : current.getDepartmentId();
            if (teamId == null) {
                teamId = teamMembershipRepository.findByUserIdAndDeletedAtIsNull(user.getUserId())
                        .stream()
                        .map(UserTeamMembership::getTeamId)
                        .findFirst()
                        .orElse(null);
            }
        }
        if (teamId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队知识库必须选择所属团队");
        }

        Department team = departmentRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "团队不存在"));
        if (team.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队已停用");
        }
        if (!user.isAdmin() && !userCanCreateTeamKbUnder(user.getUserId(), team.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能在自己所属团队或其上级团队下创建团队知识库");
        }
        return team.getId();
    }

    private boolean userCanCreateTeamKbUnder(Long userId, Long requestedTeamId) {
        UserAccount current = userRepository.findById(userId).orElse(null);
        if (current != null && isSameOrChildTeam(current.getDepartmentId(), requestedTeamId)) {
            return true;
        }
        return teamMembershipRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(UserTeamMembership::getTeamId)
                .anyMatch(userTeamId -> isSameOrChildTeam(userTeamId, requestedTeamId));
    }

    private MemberRole ensureOwnerMember(KnowledgeBase kb) {
        return memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), kb.getOwnerId())
                .map(KnowledgeBaseMember::getRole)
                .orElseGet(() -> {
                    KnowledgeBaseMember ownerMember = new KnowledgeBaseMember();
                    ownerMember.setId(idGenerator.nextId());
                    ownerMember.setKbId(kb.getId());
                    ownerMember.setUserId(kb.getOwnerId());
                    ownerMember.setRole(MemberRole.ADMIN);
                    memberRepository.save(ownerMember);
                    return MemberRole.ADMIN;
                });
    }

    private void ensureCanRemoveAdmin(Long kbId, Long adminUserId) {
        long adminCount = memberRepository.countByKbIdAndRoleAndDeletedAtIsNull(kbId, MemberRole.ADMIN);
        if (adminCount <= 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Each knowledge base must keep at least one manager");
        }
        boolean removingKnownAdmin = memberRepository.findByKbIdAndDeletedAtIsNull(kbId).stream()
                .anyMatch(member -> member.getRole() == MemberRole.ADMIN && member.getUserId().equals(adminUserId));
        if (!removingKnownAdmin) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Each knowledge base must keep at least one manager");
        }
    }

    private Optional<MemberRole> memberRole(Long kbId, Long userId) {
        return memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kbId, userId)
                .map(KnowledgeBaseMember::getRole);
    }

    private String roleFor(KnowledgeBase kb, CurrentUser user) {
        return memberRole(kb.getId(), user.getUserId())
                .map(MemberRole::name)
                .orElse(MemberRole.READER.name());
    }

    private Long resolveInviteUserId(InviteMemberRequest request) {
        if (request.getUserId() != null) {
            return request.getUserId();
        }
        String key = request.getUsernameOrEmail();
        if (key == null || key.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "User id, username or email is required");
        }
        return userRepository.findByUsername(key)
                .or(() -> userRepository.findByEmail(key))
                .map(UserAccount::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
    }

    private KbMemberDetailResponse toMemberDetail(UserAccount user, MemberRole memberRole) {
        String departmentName = departmentName(user.getDepartmentId());
        return KbMemberDetailResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .displayName(firstNonBlank(user.getNickname(), user.getUsername(), String.valueOf(user.getId())))
                .avatarUrl(user.getAvatarUrl())
                .departmentId(user.getDepartmentId())
                .departmentName(firstNonBlank(departmentName, "未分配团队"))
                .positionName(resolvePositionName(user))
                .role(memberRole)
                .build();
    }

    private String departmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .filter(dept -> dept.getDeletedAt() == null)
                .map(Department::getName)
                .orElse(null);
    }

    private String resolvePositionName(UserAccount user) {
        if (user.getDepartmentId() == null) {
            return "未分配职位";
        }
        Department dept = departmentRepository.findById(user.getDepartmentId()).orElse(null);
        if (dept != null && user.getId().equals(dept.getManagerId())) {
            return "团队负责人";
        }
        return "团队成员";
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

    private String teamName(Long teamId) {
        if (teamId == null) {
            return null;
        }
        return departmentRepository.findById(teamId)
                .filter(team -> team.getDeletedAt() == null)
                .map(Department::getName)
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase kb, String myRole) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .type(kb.getType())
                .description(kb.getDescription())
                .ownerId(kb.getOwnerId())
                .parentId(kb.getParentId())
                .teamId(effectiveTeamId(kb))
                .teamName(teamName(effectiveTeamId(kb)))
                .myRole(myRole)
                .build();
    }
}
