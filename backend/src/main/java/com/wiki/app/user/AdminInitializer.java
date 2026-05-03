package com.wiki.app.user;

import com.wiki.app.common.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String nickname;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            SnowflakeIdGenerator idGenerator,
                            @Value("${wiki.admin-bootstrap-enabled:true}") boolean enabled,
                            @Value("${wiki.admin-username:admin}") String username,
                            @Value("${wiki.admin-password:Admin@123456}") String password,
                            @Value("${wiki.admin-nickname:System Administrator}") String nickname) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    @PostConstruct
    public void initAdmin() {
        if (!enabled) {
            return;
        }

        userRepository.findByUsernameOrEmailOrPhone(username, username, username)
                .ifPresentOrElse(exist -> {
                    if (exist.getRole() != UserRole.ADMIN) {
                        exist.setRole(UserRole.ADMIN);
                        if (exist.getNickname() == null || exist.getNickname().isBlank()) {
                            exist.setNickname(nickname);
                        }
                        userRepository.save(exist);
                    }
                }, () -> {
                    UserAccount admin = new UserAccount();
                    admin.setId(idGenerator.nextId());
                    admin.setUsername(username);
                    admin.setNickname(nickname);
                    admin.setPasswordHash(passwordEncoder.encode(password));
                    admin.setRole(UserRole.ADMIN);
                    userRepository.save(admin);
                });
    }
}
