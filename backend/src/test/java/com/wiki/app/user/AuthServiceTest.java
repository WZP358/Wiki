package com.wiki.app.user;

import com.wiki.app.common.BusinessException;
import com.wiki.app.common.ErrorCode;
import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.log.OperationLogService;
import com.wiki.app.security.JwtTokenProvider;
import com.wiki.app.user.dto.LoginRequest;
import com.wiki.app.user.dto.LoginResponse;
import com.wiki.app.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VerifyCodeService verifyCodeService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private SnowflakeIdGenerator idGenerator;
    @Mock
    private OperationLogService operationLogService;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private UserTeamMembershipRepository teamMembershipRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                verifyCodeService,
                passwordEncoder,
                jwtTokenProvider,
                idGenerator,
                operationLogService,
                departmentRepository,
                teamMembershipRepository
        );
    }

    @Test
    void loginCreatesTokenAndProfileForValidCredentials() {
        UserAccount user = new UserAccount();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("hash");
        user.setNickname("Administrator");
        user.setRole(UserRole.ADMIN);

        LoginRequest request = new LoginRequest();
        request.setAccount("admin");
        request.setPassword("Admin@123456");

        when(userRepository.findByUsernameOrEmailOrPhone("admin", "admin", "admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@123456", "hash")).thenReturn(true);
        when(jwtTokenProvider.createToken(1L, "admin", "ADMIN")).thenReturn("jwt-token");
        when(teamMembershipRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        LoginResponse response = authService.login(request, "127.0.0.1");

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getId()).isEqualTo(1L);
        assertThat(response.getUser().getUsername()).isEqualTo("admin");
        assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
        verify(operationLogService).record(1L, "admin", "LOGIN", "USER", "1", "127.0.0.1", "User login");
    }

    @Test
    void registerRejectsExternalAvatarUrl() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("Password@123");
        request.setEmail("alice@example.com");
        request.setCode("123456");
        request.setAvatarUrl("https://example.com/avatar.png");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        verify(verifyCodeService).validate("register", "alice@example.com", "123456");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }

    @Test
    void registerStoresDemoPasswordForAdminDisplay() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("product_manager");
        request.setPassword("Demo@123456");
        request.setEmail("product.manager@example.com");
        request.setCode("123456");

        when(userRepository.existsByUsername("product_manager")).thenReturn(false);
        when(userRepository.existsByEmail("product.manager@example.com")).thenReturn(false);
        when(idGenerator.nextId()).thenReturn(10L);
        when(passwordEncoder.encode("Demo@123456")).thenReturn("hash");
        when(jwtTokenProvider.createToken(10L, "product_manager", "USER")).thenReturn("token");
        when(teamMembershipRepository.findByUserIdAndDeletedAtIsNull(10L)).thenReturn(List.of());

        authService.register(request, "127.0.0.1");

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().getDemoPassword()).isEqualTo("Demo@123456");
        assertThat(captor.getValue().getNickname()).isEqualTo("product_manager");
    }

    @Test
    void registerRejectsDuplicateUsernameBeforeSaving() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("Password@123");
        request.setPhone("13800138000");
        request.setCode("123456");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);

        verify(verifyCodeService).validate("register", "13800138000", "123456");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(UserAccount.class));
    }
}
