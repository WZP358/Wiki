package com.wiki.app.user;

import com.wiki.app.common.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInitializerTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Test
    void existingAdminGetsMeaningfulNicknameAndDemoPassword() {
        UserAccount admin = new UserAccount();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setNickname("System Administrator");
        admin.setRole(UserRole.ADMIN);
        admin.setPasswordHash("hash");

        when(userRepository.findByUsernameOrEmailOrPhone("admin", "admin", "admin")).thenReturn(Optional.of(admin));

        new AdminInitializer(userRepository, passwordEncoder, idGenerator, true,
                "admin", "Admin@123456", "平台管理员").initAdmin();

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("平台管理员");
        assertThat(captor.getValue().getDemoPassword()).isEqualTo("Admin@123456");
    }

    @Test
    void createdAdminStoresDemoPassword() {
        when(userRepository.findByUsernameOrEmailOrPhone("admin", "admin", "admin")).thenReturn(Optional.empty());
        when(idGenerator.nextId()).thenReturn(1L);
        when(passwordEncoder.encode("Admin@123456")).thenReturn("hash");

        new AdminInitializer(userRepository, passwordEncoder, idGenerator, true,
                "admin", "Admin@123456", "平台管理员").initAdmin();

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getNickname()).isEqualTo("平台管理员");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().getDemoPassword()).isEqualTo("Admin@123456");
    }
}
