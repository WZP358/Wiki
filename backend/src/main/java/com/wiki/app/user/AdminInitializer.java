package com.wiki.app.user;

import com.wiki.app.common.SnowflakeIdGenerator;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;

    public AdminInitializer(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            SnowflakeIdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
    }

    @PostConstruct
    public void initAdmin() {
        if (userRepository.existsByUsername("admin")) {
            return;
        }

        UserAccount admin = new UserAccount();
        admin.setId(idGenerator.nextId());
        admin.setUsername("admin");
        admin.setNickname("系统管理员");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123456"));
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);
    }
}
