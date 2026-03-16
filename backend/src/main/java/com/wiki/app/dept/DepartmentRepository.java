package com.wiki.app.dept;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findByDeletedAtIsNull();

    List<Department> findByParentIdAndDeletedAtIsNull(Long parentId);

    List<Department> findByManagerIdAndDeletedAtIsNull(Long managerId);
}
