package com.wiki.app.dept;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DepartmentInitializer {
    private final DepartmentRepository departmentRepository;

    public DepartmentInitializer(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @PostConstruct
    public void initTeamTree() {
        if (departmentRepository.count() > 0) {
            return;
        }

        save(100L, null, "Wiki 项目团队", "企业知识库项目的顶层团队");
        save(101L, 100L, "平台研发组", "负责 Wiki 系统核心研发");
        save(102L, 100L, "知识运营组", "负责知识沉淀、模板和内容治理");
        save(103L, 101L, "前端子团队", "负责用户端和后台管理界面");
        save(104L, 101L, "后端子团队", "负责接口、权限、缓存和数据模型");
        save(105L, 101L, "测试与质量子团队", "负责测试用例、验收和质量保障");
        save(106L, 101L, "运维与部署子团队", "负责环境、部署、监控和数据备份");
        save(107L, 104L, "权限与用户任务组", "负责登录注册、用户分配和协作权限");
        save(108L, 104L, "文档与版本任务组", "负责文档树、版本历史、回滚和对比");
        save(109L, 104L, "缓存与搜索任务组", "负责 Redis 缓存、编辑锁和 LIKE 检索");
    }

    private void save(Long id, Long parentId, String name, String description) {
        Department department = new Department();
        department.setId(id);
        department.setParentId(parentId);
        department.setName(name);
        department.setDescription(description);
        departmentRepository.save(department);
    }
}
