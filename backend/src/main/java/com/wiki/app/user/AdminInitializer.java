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
                    boolean changed = false;
                    if (exist.getRole() != UserRole.ADMIN) {
                        exist.setRole(UserRole.ADMIN);
                        changed = true;
                    }
                    if (exist.getNickname() == null || exist.getNickname().isBlank()
                            || "System Administrator".equals(exist.getNickname())
                            || username.equals(exist.getNickname())) {
                        exist.setNickname(nickname);
                        changed = true;
                    }
                    if (exist.getDemoPassword() == null || exist.getDemoPassword().isBlank()) {
                        exist.setDemoPassword(password);
                        changed = true;
                    }
                    if (exist.getEmail() == null || exist.getEmail().isBlank()) {
                        exist.setEmail("admin@example.com");
                        changed = true;
                    }
                    if (exist.getPhone() == null || exist.getPhone().isBlank()) {
                        exist.setPhone("13900020000");
                        changed = true;
                    }
                    if (changed) {
                        userRepository.save(exist);
                    }
                }, () -> {
                    UserAccount admin = new UserAccount();
                    admin.setId(idGenerator.nextId());
                    admin.setUsername(username);
                    admin.setNickname(nickname);
                    admin.setPasswordHash(passwordEncoder.encode(password));
                    admin.setDemoPassword(password);
                    admin.setEmail("admin@example.com");
                    admin.setPhone("13900020000");
                    admin.setRole(UserRole.ADMIN);
                    userRepository.save(admin);
                });
    }
}
