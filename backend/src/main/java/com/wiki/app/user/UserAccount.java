package com.wiki.app.user;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email", unique = true),
        @Index(name = "idx_users_phone", columnList = "phone", unique = true),
        @Index(name = "idx_user_dept", columnList = "department_id")
})
public class UserAccount extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(unique = true, length = 128)
    private String email;

    @Column(unique = true, length = 32)
    private String phone;

    @Column(nullable = false, length = 128)
    private String passwordHash;

    @Column(length = 64)
    private String nickname;

    @Column(length = 512)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;

    @Column(name = "department_id")
    private Long departmentId;
}
