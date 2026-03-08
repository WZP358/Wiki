package com.wiki.app.log;

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
@Table(name = "operation_logs", indexes = {
        @Index(name = "idx_logs_created_at", columnList = "created_at"),
        @Index(name = "idx_logs_user_id", columnList = "user_id")
})
public class OperationLog extends BaseEntity {
    @Id
    private Long id;

    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String action;

    @Column(length = 64)
    private String targetType;

    @Column(length = 64)
    private String targetId;

    @Column(length = 64)
    private String ip;

    @Column(length = 1024)
    private String detail;
}
