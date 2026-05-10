package com.wiki.app.kb;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.dto.CreateKnowledgeBaseRequest;
import com.wiki.app.kb.dto.InviteMemberRequest;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceBusinessTest {

    @Mock
    private KnowledgeBaseRepository kbRepository;
    @Mock
    private KnowledgeBaseMemberRepository memberRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTeamMembershipRepository teamMembershipRepository;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private KnowledgeBaseMemberSyncService memberSyncService;

    private KnowledgeBaseService service;
    private final CurrentUser alice = new CurrentUser(10L, "alice", "USER");

    @BeforeEach
    void setUp() {
        service = new KnowledgeBaseService(
                kbRepository,
                memberRepository,
                departmentRepository,
                userRepository,
                teamMembershipRepository,
                idGenerator,
                operationLogService,
                memberSyncService
        );
    }

    @Test
    void createDepartmentKnowledgeBaseRequiresUserTeamMembership() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest();
        request.setName("Team KB");
        request.setType(KnowledgeBaseType.DEPARTMENT);
        request.setTeamId(20L);

        UserAccount user = user(10L, 30L);
        Department requestedTeam = department(20L, null);
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(20L)).thenReturn(Optional.of(requestedTeam));
        when(teamMembershipRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(request, alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void companyKnowledgeBaseIsVisibleToNormalUsersAsReader() {
        KnowledgeBase kb = kb(1L, 99L, KnowledgeBaseType.COMPANY, null);
        when(kbRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.empty());

        assertThat(service.getForCurrent(1L, alice).getMyRole()).isEqualTo(MemberRole.READER.name());
    }

    @Test
    void cannotDemoteTheOnlyKnowledgeBaseAdmin() {
        KnowledgeBase kb = kb(1L, 10L, KnowledgeBaseType.COMPANY, null);
        KnowledgeBaseMember adminMember = member(1L, 10L, MemberRole.ADMIN);
        UserAccount invited = user(10L, null);
        invited.setRole(UserRole.USER);
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUserId(10L);
        request.setRole(MemberRole.READER);

        when(kbRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(1L, 10L))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findById(10L)).thenReturn(Optional.of(invited));
        when(memberRepository.countByKbIdAndRoleAndDeletedAtIsNull(1L, MemberRole.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.upsertMember(1L, request, alice, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void inviteByUsernameCreatesMemberWhenCurrentUserIsKbAdmin() {
        KnowledgeBase kb = kb(1L, 10L, KnowledgeBaseType.COMPANY, null);
        KnowledgeBaseMember adminMember = member(1L, 10L, MemberRole.ADMIN);
        UserAccount bob = user(11L, null);
        bob.setUsername("bob");
        bob.setRole(UserRole.USER);
        InviteMemberRequest request = new InviteMemberRequest();
        request.setUsernameOrEmail("bob");
        request.setRole(MemberRole.EDITOR);

        when(kbRepository.findById(1L)).thenReturn(Optional.of(kb));
        when(memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(1L, 10L))
                .thenReturn(Optional.of(adminMember));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findById(11L)).thenReturn(Optional.of(bob));
        when(memberRepository.findByKbIdAndUserIdAndDeletedAtIsNull(1L, 11L)).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(200L);

        assertThat(service.upsertMember(1L, request, alice, "127.0.0.1").getRole()).isEqualTo(MemberRole.EDITOR);
        verify(memberRepository).save(any(KnowledgeBaseMember.class));
    }

    private KnowledgeBase kb(Long id, Long ownerId, KnowledgeBaseType type, Long teamId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName("KB");
        kb.setOwnerId(ownerId);
        kb.setType(type);
        kb.setTeamId(teamId);
        return kb;
    }

    private KnowledgeBaseMember member(Long kbId, Long userId, MemberRole role) {
        KnowledgeBaseMember member = new KnowledgeBaseMember();
        member.setId(100L);
        member.setKbId(kbId);
        member.setUserId(userId);
        member.setRole(role);
        return member;
    }

    private UserAccount user(Long id, Long departmentId) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("user" + id);
        user.setNickname("User " + id);
        user.setRole(UserRole.USER);
        user.setDepartmentId(departmentId);
        return user;
    }

    private Department department(Long id, Long parentId) {
        Department department = new Department();
        department.setId(id);
        department.setName("Department " + id);
        department.setParentId(parentId);
        return department;
    }
}
