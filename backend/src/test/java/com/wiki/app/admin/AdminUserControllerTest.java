package com.wiki.app.admin;

import com.wiki.app.admin.user.AdminUserController;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseMemberSyncService;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembershipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private KnowledgeBaseRepository kbRepository;
    @Mock
    private KnowledgeBaseMemberRepository memberRepository;
    @Mock
    private UserTeamMembershipRepository teamMembershipRepository;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private KnowledgeBaseMemberSyncService memberSyncService;

    @Test
    void listDefaultsToActiveUsersOnly() {
        AdminUserController controller = controller();
        when(userRepository.adminSearch(isNull(), isNull(), isNull(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(1L, null))));
        when(teamMembershipRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        controller.list(null, null, null, null, 0, 20);

        ArgumentCaptor<Boolean> activeCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(userRepository).adminSearch(isNull(), isNull(), isNull(), activeCaptor.capture(), any(Pageable.class));
        assertThat(activeCaptor.getValue()).isTrue();
    }

    @Test
    void listStillAllowsExplicitInactiveFilter() {
        AdminUserController controller = controller();
        when(userRepository.adminSearch(isNull(), isNull(), isNull(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user(2L, LocalDateTime.now()))));
        when(teamMembershipRepository.findByUserIdAndDeletedAtIsNull(2L)).thenReturn(List.of());

        controller.list(null, null, null, false, 0, 20);

        ArgumentCaptor<Boolean> activeCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(userRepository).adminSearch(isNull(), isNull(), isNull(), activeCaptor.capture(), any(Pageable.class));
        assertThat(activeCaptor.getValue()).isFalse();
    }

    private AdminUserController controller() {
        return new AdminUserController(
                userRepository,
                departmentRepository,
                kbRepository,
                memberRepository,
                teamMembershipRepository,
                idGenerator,
                memberSyncService
        );
    }

    private UserAccount user(Long id, LocalDateTime deletedAt) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setUsername("demo" + id);
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeletedAt(deletedAt);
        return user;
    }
}
