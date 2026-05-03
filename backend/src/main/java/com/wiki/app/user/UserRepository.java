package com.wiki.app.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByPhone(String phone);

    Optional<UserAccount> findByUsernameOrEmailOrPhone(String username, String email, String phone);

    long countByDeletedAtIsNull();

    @Query("""
            select count(u) from UserAccount u
            where u.role = :role
            and u.departmentId is null
            and not exists (
                select 1 from UserTeamMembership m
                where m.userId = u.id and m.deletedAt is null
            )
            and u.deletedAt is null
            """)
    long countPendingAssignment(@Param("role") UserRole role);

    @Query("""
            select u from UserAccount u
            where (:keyword is null or :keyword = ''
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%'))
            )
            and (:role is null or u.role = :role)
            and (:departmentId is null or u.departmentId = :departmentId
                or exists (
                    select 1 from UserTeamMembership m
                    where m.userId = u.id
                    and m.teamId = :departmentId
                    and m.deletedAt is null
                )
            )
            and (:active is null
                 or (:active = true and u.deletedAt is null)
                 or (:active = false and u.deletedAt is not null)
            )
            """)
    Page<UserAccount> adminSearch(@Param("keyword") String keyword,
                                 @Param("role") UserRole role,
                                 @Param("departmentId") Long departmentId,
                                 @Param("active") Boolean active,
                                 Pageable pageable);

    @Query("""
            select u from UserAccount u
            where u.role = com.wiki.app.user.UserRole.USER
            and u.departmentId is null
            and not exists (
                select 1 from UserTeamMembership m
                where m.userId = u.id and m.deletedAt is null
            )
            and u.deletedAt is null
            and (:keyword is null or :keyword = ''
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%'))
            )
            """)
    Page<UserAccount> pendingAssignment(@Param("keyword") String keyword, Pageable pageable);
}
