package com.wiki.app.admin.user;

import com.wiki.app.admin.user.dto.AdminUserResponse;
import com.wiki.app.admin.user.dto.AssignPendingUserRequest;
import com.wiki.app.admin.user.dto.UpdateUserAdminRequest;
import com.wiki.app.common.ApiResponse;
import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseMember;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseMemberSyncService;
import com.wiki.app.kb.MemberRole;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembership;
import com.wiki.app.user.UserTeamMembershipRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final UserTeamMembershipRepository teamMembershipRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public AdminUserController(UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               KnowledgeBaseRepository kbRepository,
                               KnowledgeBaseMemberRepository memberRepository,
                               UserTeamMembershipRepository teamMembershipRepository,
                               SnowflakeIdGenerator idGenerator,
                               KnowledgeBaseMemberSyncService memberSyncService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.kbRepository = kbRepository;
        this.memberRepository = memberRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.idGenerator = idGenerator;
        this.memberSyncService = memberSyncService;
    }

    @GetMapping
    public ApiResponse<Page<AdminUserResponse>> list(@RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String role,
                                                     @RequestParam(required = false) Long departmentId,
                                                     @RequestParam(required = false) Boolean active,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "20") int size) {
        UserRole roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = UserRole.valueOf(role.trim().toUpperCase());
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid role");
            }
        }

        Boolean activeFilter = active == null ? Boolean.TRUE : active;
        Page<UserAccount> result = userRepository.adminSearch(
                keyword == null ? null : keyword.trim(),
                roleEnum,
                departmentId,
                activeFilter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return ApiResponse.ok(result.map(this::toResponse));
    }

    @GetMapping("/pending")
    public ApiResponse<Page<AdminUserResponse>> pending(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        Page<UserAccount> result = userRepository.pendingAssignment(
                keyword == null ? null : keyword.trim(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ApiResponse.ok(result.map(this::toResponse));
    }

    @GetMapping("/pending/count")
    public ApiResponse<Long> pendingCount() {
        return ApiResponse.ok(userRepository.countPendingAssignment(UserRole.USER));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> get(@PathVariable Long userId) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        return ApiResponse.ok(toResponse(user));
    }

    @PostMapping("/assign-pending")
    @Transactional
    public ApiResponse<AdminUserResponse> assignPending(@Valid @RequestBody AssignPendingUserRequest request) {
        UserAccount user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
        List<Long> teamIds = normalizeTeamIds(request.getTeamIds(), request.getDepartmentId());
        assignTeams(user, teamIds);
        userRepository.save(user);

        if (request.getKbId() != null) {
            KnowledgeBase kb = kbRepository.findById(request.getKbId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found"));
            if (kb.getDeletedAt() != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Knowledge base is disabled");
            }
            MemberRole role = request.getMemberRole() == null ? MemberRole.READER : request.getMemberRole();
            KnowledgeBaseMember member = memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(kb.getId(), user.getId())
                    .orElseGet(() -> {
                        KnowledgeBaseMember created = new KnowledgeBaseMember();
                        created.setId(idGenerator.nextId());
                        created.setKbId(kb.getId());
                        created.setUserId(user.getId());
                        return created;
                    });
            member.setRole(role);
            member.setDeletedAt(null);
            memberRepository.save(member);
        }
        memberSyncService.syncUserAutoReaders(user.getId());

        return ApiResponse.ok(toResponse(user));
    }

    @PostMapping("/update")
    @Transactional
    public ApiResponse<AdminUserResponse> update(@Valid @RequestBody UpdateUserAdminRequest request) {
        UserAccount user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));

        UserRole nextRole = user.getRole();
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                nextRole = UserRole.valueOf(request.getRole().trim().toUpperCase());
                user.setRole(nextRole);
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid role");
            }
        }

        if (nextRole == UserRole.ADMIN) {
            clearTeams(user);
        } else if (request.getTeamIds() != null || request.getDepartmentId() != null) {
            assignTeams(user, normalizeTeamIds(request.getTeamIds(), request.getDepartmentId()));
        }
        if (request.getActive() != null) {
            user.setDeletedAt(Boolean.TRUE.equals(request.getActive()) ? null : LocalDateTime.now());
        }

        userRepository.save(user);
        memberSyncService.syncUserAutoReaders(user.getId());
        return ApiResponse.ok(toResponse(user));
    }

    private AdminUserResponse toResponse(UserAccount u) {
        return AdminUserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phone(u.getPhone())
                .nickname(u.getNickname())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole() == null ? null : u.getRole().name())
                .departmentId(u.getDepartmentId())
                .departmentName(u.getRole() == UserRole.ADMIN && u.getDepartmentId() == null
                        ? "系统管理"
                        : departmentName(u.getDepartmentId()))
                .teamIds(teamIds(u.getId()))
                .teamNames(u.getRole() == UserRole.ADMIN ? List.of("系统管理") : teamNames(u.getId()))
                .active(u.getDeletedAt() == null)
                .pendingAssignment(u.getRole() == UserRole.USER
                        && u.getDepartmentId() == null
                        && teamMembershipRepository.countByUserIdAndDeletedAtIsNull(u.getId()) == 0
                        && u.getDeletedAt() == null)
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    private List<Long> normalizeTeamIds(List<Long> teamIds, Long fallbackDepartmentId) {
        Set<Long> result = new LinkedHashSet<>();
        if (teamIds != null) {
            for (Long teamId : teamIds) {
                if (teamId != null) {
                    result.add(teamId);
                }
            }
        }
        if (result.isEmpty() && fallbackDepartmentId != null) {
            result.add(fallbackDepartmentId);
        }
        if (result.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请至少选择一个细分团队");
        }
        return new ArrayList<>(result);
    }

    private void assignTeams(UserAccount user, List<Long> teamIds) {
        for (Long teamId : teamIds) {
            validateLeafTeam(teamId);
        }

        for (UserTeamMembership existing : teamMembershipRepository.findByUserIdAndDeletedAtIsNull(user.getId())) {
            if (!teamIds.contains(existing.getTeamId())) {
                existing.setDeletedAt(LocalDateTime.now());
                teamMembershipRepository.save(existing);
            }
        }

        for (Long teamId : teamIds) {
            UserTeamMembership membership = teamMembershipRepository.findByUserIdAndTeamIdAndDeletedAtIsNull(user.getId(), teamId)
                    .orElseGet(() -> {
                        UserTeamMembership created = new UserTeamMembership();
                        created.setId(idGenerator.nextId());
                        created.setUserId(user.getId());
                        created.setTeamId(teamId);
                        return created;
                    });
            membership.setDeletedAt(null);
            teamMembershipRepository.save(membership);
        }
        user.setDepartmentId(teamIds.get(0));
    }

    private void clearTeams(UserAccount user) {
        for (UserTeamMembership existing : teamMembershipRepository.findByUserIdAndDeletedAtIsNull(user.getId())) {
            existing.setDeletedAt(LocalDateTime.now());
            teamMembershipRepository.save(existing);
        }
        user.setDepartmentId(null);
    }

    private void validateLeafTeam(Long teamId) {
        Department team = departmentRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "团队不存在"));
        if (team.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "团队已停用");
        }
        if (!departmentRepository.findByParentIdAndDeletedAtIsNull(teamId).isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只能分配到最细分的团队，请选择没有下级团队的节点");
        }
    }

    private List<Long> teamIds(Long userId) {
        return teamMembershipRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(UserTeamMembership::getTeamId)
                .toList();
    }

    private List<String> teamNames(Long userId) {
        return teamMembershipRepository.findByUserIdAndDeletedAtIsNull(userId)
                .stream()
                .map(membership -> departmentName(membership.getTeamId()))
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private String departmentName(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return departmentRepository.findById(departmentId)
                .map(Department::getName)
                .orElse(null);
    }
}

