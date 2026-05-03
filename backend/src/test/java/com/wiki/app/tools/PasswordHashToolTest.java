package com.wiki.app.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility test to generate BCrypt hashes for manual password reset.
 * Run:
 *   mvn -q -Dtest=PasswordHashToolTest test
 */
public class PasswordHashToolTest {

    @Test
    @Disabled("Manual utility only. Run explicitly after removing @Disabled locally.")
    void printBcryptHash() {
        String password = System.getProperty("pw");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Pass -Dpw=your-password when running this utility");
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        System.out.println("BCRYPT=" + hash);
    }
}

