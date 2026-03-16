package com.wiki.app.dept;

import com.wiki.app.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "departments", indexes = {
        @Index(name = "idx_dept_parent", columnList = "parent_id"),
        @Index(name = "idx_dept_manager", columnList = "manager_id")
})
public class Department extends BaseEntity {
    @Id
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(length = 512)
    private String description;
}
