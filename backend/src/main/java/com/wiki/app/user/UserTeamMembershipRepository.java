package com.wiki.app.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTeamMembershipRepository extends JpaRepository<UserTeamMembership, Long> {
    List<UserTeamMembership> findByUserIdAndDeletedAtIsNull(Long userId);

    List<UserTeamMembership> findByTeamIdAndDeletedAtIsNull(Long teamId);

    Optional<UserTeamMembership> findByUserIdAndTeamIdAndDeletedAtIsNull(Long userId, Long teamId);

    long countByUserIdAndDeletedAtIsNull(Long userId);
}
