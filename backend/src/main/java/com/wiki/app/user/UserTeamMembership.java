package com.wiki.app.user;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_team_memberships", indexes = {
        @Index(name = "idx_user_team_user", columnList = "user_id"),
        @Index(name = "idx_user_team_team", columnList = "team_id"),
        @Index(name = "idx_user_team_unique", columnList = "user_id,team_id")
})
public class UserTeamMembership extends BaseEntity {
    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "team_id", nullable = false)
    private Long teamId;
}
