package com.wiki.app.config;

import com.wiki.app.common.SnowflakeIdGenerator;
import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.doc.DocVisibility;
import com.wiki.app.doc.LocalDocStorageService;
import com.wiki.app.doc.MarkdownService;
import com.wiki.app.doc.WikiDocument;
import com.wiki.app.doc.WikiDocumentRepository;
import com.wiki.app.kb.KnowledgeBase;
import com.wiki.app.kb.KnowledgeBaseMember;
import com.wiki.app.kb.KnowledgeBaseMemberRepository;
import com.wiki.app.kb.KnowledgeBaseRepository;
import com.wiki.app.kb.KnowledgeBaseType;
import com.wiki.app.kb.MemberRole;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import com.wiki.app.user.UserTeamMembership;
import com.wiki.app.user.UserTeamMembershipRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemoDataInitializer {
    private static final String DEMO_PASSWORD = "Demo@123456";

    private final boolean enabled;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserTeamMembershipRepository teamMembershipRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository memberRepository;
    private final WikiDocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGenerator idGenerator;
    private final MarkdownService markdownService;
    private final LocalDocStorageService storageService;

    public DemoDataInitializer(@Value("${wiki.demo-data-enabled:true}") boolean enabled,
                               UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               UserTeamMembershipRepository teamMembershipRepository,
                               KnowledgeBaseRepository knowledgeBaseRepository,
                               KnowledgeBaseMemberRepository memberRepository,
                               WikiDocumentRepository documentRepository,
                               PasswordEncoder passwordEncoder,
                               SnowflakeIdGenerator idGenerator,
                               MarkdownService markdownService,
                               LocalDocStorageService storageService) {
        this.enabled = enabled;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.memberRepository = memberRepository;
        this.documentRepository = documentRepository;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.markdownService = markdownService;
        this.storageService = storageService;
    }

    @PostConstruct
    @Transactional
    public void init() {
        if (!enabled) {
            return;
        }

        cleanupGeneratedTestAccounts();
        userRepository.flush();

        Map<String, Department> depts = new LinkedHashMap<>();
        depts.put("company", department("Wiki 项目团队", null, "企业知识库项目的顶层团队"));
        depts.put("product", department("产品中心", depts.get("company"), "负责产品规划、需求管理、版本发布和用户研究"));
        depts.put("frontend", department("前端子团队", depts.get("product"), "负责用户门户、管理后台和交互体验"));
        depts.put("backend", department("后端子团队", depts.get("product"), "负责接口、权限、文档、搜索和审计能力"));
        depts.put("qa", department("测试与质量子团队", depts.get("company"), "负责测试用例、回归验证、质量门禁和发布验收"));
        depts.put("ops", department("运维与部署子团队", depts.get("company"), "负责环境部署、监控告警、备份和故障恢复"));
        depts.put("support", department("客服中心", depts.get("company"), "负责客户支持、FAQ 维护和问题升级"));
        depts.put("knowledge", department("知识运营组", depts.get("company"), "负责内容治理、模板维护和知识库运营"));
        depts.put("security", department("权限与用户任务组", depts.get("backend"), "负责登录注册、用户分配、团队权限和审计"));
        depts.put("search", department("缓存与搜索任务组", depts.get("backend"), "负责缓存、搜索、性能优化和热点数据"));

        Map<String, UserAccount> users = new LinkedHashMap<>();
        users.put("product_manager", demoUser("product_manager", "产品负责人", "product.manager@example.com", "13900020001", depts.get("product")));
        users.put("ux_designer", demoUser("ux_designer", "体验设计师", "ux.designer@example.com", "13900020002", depts.get("frontend")));
        users.put("frontend_lead", demoUser("frontend_lead", "前端负责人", "frontend.lead@example.com", "13900020003", depts.get("frontend")));
        users.put("backend_lead", demoUser("backend_lead", "后端负责人", "backend.lead@example.com", "13900020004", depts.get("backend")));
        users.put("java_engineer", demoUser("java_engineer", "后端开发工程师", "java.engineer@example.com", "13900020005", depts.get("backend")));
        users.put("qa_lead", demoUser("qa_lead", "测试负责人", "qa.lead@example.com", "13900020006", depts.get("qa")));
        users.put("test_engineer", demoUser("test_engineer", "自动化测试工程师", "test.engineer@example.com", "13900020007", depts.get("qa")));
        users.put("ops_engineer", demoUser("ops_engineer", "运维工程师", "ops.engineer@example.com", "13900020008", depts.get("ops")));
        users.put("support_agent", demoUser("support_agent", "客服知识维护员", "support.agent@example.com", "13900020009", depts.get("support")));
        users.put("customer_success", demoUser("customer_success", "客户成功经理", "customer.success@example.com", "13900020010", depts.get("support")));
        users.put("knowledge_editor", demoUser("knowledge_editor", "知识库编辑", "knowledge.editor@example.com", "13900020011", depts.get("knowledge")));
        users.put("content_reviewer", demoUser("content_reviewer", "内容审核员", "content.reviewer@example.com", "13900020012", depts.get("knowledge")));
        users.put("security_admin", demoUser("security_admin", "权限管理员", "security.admin@example.com", "13900020013", depts.get("security")));
        users.put("audit_specialist", demoUser("audit_specialist", "审计专员", "audit.specialist@example.com", "13900020014", depts.get("security")));
        users.put("search_engineer", demoUser("search_engineer", "搜索工程师", "search.engineer@example.com", "13900020015", depts.get("search")));
        users.put("release_manager", demoUser("release_manager", "发布经理", "release.manager@example.com", "13900020016", depts.get("ops")));
        users.put("training_coach", demoUser("training_coach", "培训讲师", "training.coach@example.com", "13900020017", depts.get("knowledge")));
        users.put("data_analyst", demoUser("data_analyst", "数据分析师", "data.analyst@example.com", "13900020018", depts.get("product")));

        assignDepartmentManagers(depts, users);
        users.forEach((key, user) -> membership(user, depts.get(keyToDept(key))));

        KnowledgeBase company = knowledgeBase("公司制度与入职指南", KnowledgeBaseType.COMPANY, users.get("product_manager"), null,
                "面向全员的制度、流程、入职资料和跨部门协作规范。");
        KnowledgeBase productKb = knowledgeBase("产品需求与版本知识库", KnowledgeBaseType.DEPARTMENT, users.get("product_manager"), depts.get("product"),
                "沉淀需求评审、版本计划、用户反馈和产品指标。");
        KnowledgeBase frontendKb = knowledgeBase("前端工程规范库", KnowledgeBaseType.DEPARTMENT, users.get("frontend_lead"), depts.get("frontend"),
                "沉淀组件规范、页面交互、构建发布和问题排查。");
        KnowledgeBase backendKb = knowledgeBase("后端架构与接口知识库", KnowledgeBaseType.DEPARTMENT, users.get("backend_lead"), depts.get("backend"),
                "沉淀接口设计、权限模型、数据结构和服务治理。");
        KnowledgeBase qaKb = knowledgeBase("测试与质量保障知识库", KnowledgeBaseType.DEPARTMENT, users.get("qa_lead"), depts.get("qa"),
                "沉淀测试策略、回归清单、缺陷规范和质量报告。");
        KnowledgeBase opsKb = knowledgeBase("运维部署与应急知识库", KnowledgeBaseType.DEPARTMENT, users.get("ops_engineer"), depts.get("ops"),
                "沉淀部署流程、监控告警、备份恢复和应急预案。");
        KnowledgeBase supportKb = knowledgeBase("客服 FAQ 与客户成功知识库", KnowledgeBaseType.DEPARTMENT, users.get("support_agent"), depts.get("support"),
                "沉淀常见问题、标准话术、升级路径和客户案例。");
        KnowledgeBase auditKb = knowledgeBase("权限审计与安全规范库", KnowledgeBaseType.DEPARTMENT, users.get("security_admin"), depts.get("security"),
                "沉淀权限分配、账号审计、操作日志和安全检查。");
        KnowledgeBase privateKb = knowledgeBase("运营活动作战室", KnowledgeBaseType.PRIVATE, users.get("release_manager"), depts.get("ops"),
                "发布前准备、活动上线检查和复盘材料，仅活动相关成员可见。");

        grant(company, users, "product_manager", MemberRole.ADMIN, "backend_lead", MemberRole.EDITOR, "knowledge_editor", MemberRole.EDITOR,
                "support_agent", MemberRole.READER, "qa_lead", MemberRole.READER, "ops_engineer", MemberRole.READER);
        grant(productKb, users, "product_manager", MemberRole.ADMIN, "ux_designer", MemberRole.EDITOR, "data_analyst", MemberRole.EDITOR,
                "frontend_lead", MemberRole.READER, "backend_lead", MemberRole.READER);
        grant(frontendKb, users, "frontend_lead", MemberRole.ADMIN, "ux_designer", MemberRole.EDITOR, "product_manager", MemberRole.READER,
                "qa_lead", MemberRole.READER);
        grant(backendKb, users, "backend_lead", MemberRole.ADMIN, "java_engineer", MemberRole.EDITOR, "security_admin", MemberRole.EDITOR,
                "search_engineer", MemberRole.EDITOR, "qa_lead", MemberRole.READER);
        grant(qaKb, users, "qa_lead", MemberRole.ADMIN, "test_engineer", MemberRole.EDITOR, "release_manager", MemberRole.READER,
                "product_manager", MemberRole.READER);
        grant(opsKb, users, "ops_engineer", MemberRole.ADMIN, "release_manager", MemberRole.EDITOR, "backend_lead", MemberRole.READER,
                "audit_specialist", MemberRole.READER);
        grant(supportKb, users, "support_agent", MemberRole.ADMIN, "customer_success", MemberRole.EDITOR, "knowledge_editor", MemberRole.EDITOR,
                "product_manager", MemberRole.READER);
        grant(auditKb, users, "security_admin", MemberRole.ADMIN, "audit_specialist", MemberRole.EDITOR, "backend_lead", MemberRole.READER);
        grant(privateKb, users, "release_manager", MemberRole.ADMIN, "ops_engineer", MemberRole.EDITOR, "data_analyst", MemberRole.READER);

        documents(company, users.get("knowledge_editor"), List.of(
                doc("新员工入职流程", "入职前账号、部门、导师和前三天任务准备；入职当天完成账号登录、制度学习和知识库加入。"),
                doc("知识库权限说明", "公司知识库、部门知识库、私有知识库的可见范围和协作角色说明。"),
                doc("跨部门协作流程", "需求从提出、评审、研发、测试、发布到复盘的完整协作链路。"),
                doc("会议纪要模板", "适用于需求评审、技术方案评审、发布复盘和客户问题复盘。")
        ));
        documents(productKb, users.get("product_manager"), List.of(
                doc("需求评审模板", "包含业务背景、用户场景、权限边界、验收标准和数据指标。"),
                doc("版本发布清单", "发布前确认需求关闭、测试通过、配置同步、公告准备和回滚方案。"),
                doc("用户反馈分级规则", "将反馈分为缺陷、体验优化、功能需求、咨询问题四类。"),
                doc("产品指标口径", "定义活跃用户、知识库创建数、文档阅读量、收藏数和分享转化。")
        ));
        documents(frontendKb, users.get("frontend_lead"), List.of(
                doc("页面布局规范", "统一后台表格、树侧栏、抽屉、弹窗和空状态的展示规则。"),
                doc("若依组件复用规范", "后台提示框、确认框、表格工具栏、树侧栏统一复用若依风格组件。"),
                doc("前端路由与权限说明", "用户端、管理端、公开分享页和登录页的路由边界。"),
                doc("构建发布检查", "发布前执行依赖检查、生产构建、浏览器冒烟和资源体积确认。")
        ));
        documents(backendKb, users.get("backend_lead"), List.of(
                doc("接口响应结构规范", "所有接口统一返回 success、code、message、data 或若依兼容结构。"),
                doc("文档可见性校验", "评论、收藏、点赞、分享前必须校验当前用户是否可读该文档。"),
                doc("知识库成员角色模型", "ADMIN 可管理成员，EDITOR 可编辑文档，READER 仅可阅读。"),
                doc("操作日志审计", "登录、注册、知识库变更、文档编辑、成员变更写入操作日志。")
        ));
        documents(qaKb, users.get("qa_lead"), List.of(
                doc("回归测试清单", "覆盖登录、注册、知识库、文档树、编辑器、评论、收藏、分享和后台管理。"),
                doc("缺陷等级定义", "P0 阻断主流程，P1 影响核心功能，P2 影响体验，P3 优化建议。"),
                doc("发布验收报告模板", "记录测试范围、风险项、未关闭问题、回滚建议和验收结论。"),
                doc("自动化测试策略", "后端业务单测、API 冒烟、前端构建和 Playwright E2E 分层覆盖。")
        ));
        documents(opsKb, users.get("ops_engineer"), List.of(
                doc("部署上线 SOP", "从打包、环境变量、数据库备份、启动检查到发布确认的步骤。"),
                doc("端口占用排查", "定位 8080、5173、5181、5184 等端口占用并确认进程来源。"),
                doc("数据库备份恢复", "发布前备份，故障时按时间点恢复，并记录恢复负责人。"),
                doc("监控告警处理", "按接口错误率、CPU、内存、数据库连接和磁盘空间分级处理。")
        ));
        documents(supportKb, users.get("support_agent"), List.of(
                doc("客户常见问题 FAQ", "登录失败、看不到知识库、无法编辑文档、验证码收不到的标准排查。"),
                doc("标准回复话术", "统一客户沟通口径，避免技术术语过多导致理解成本上升。"),
                doc("问题升级流程", "客服先定位账号、权限、浏览器和时间点，再升级到产品或研发。"),
                doc("客户培训大纲", "围绕知识库创建、成员邀请、文档编辑、评论收藏和分享演示。")
        ));
        documents(auditKb, users.get("security_admin"), List.of(
                doc("账号权限审计清单", "定期检查管理员、未分配用户、离职账号和异常登录记录。"),
                doc("后台用户管理说明", "用户昵称、部门、手机号、状态、演示密码和角色分配的管理口径。"),
                doc("私有文档访问规则", "非成员访问私有未发布文档时必须返回 FORBIDDEN。"),
                doc("安全配置基线", "JWT 密钥、数据库账号、跨域配置和生产环境演示数据开关。")
        ));
        documents(privateKb, users.get("release_manager"), List.of(
                doc("活动上线检查表", "活动页面、运营配置、埋点、客服 FAQ、回滚负责人全部确认后上线。"),
                doc("运营复盘模板", "记录活动目标、曝光、转化、问题、用户反馈和下次优化计划。"),
                doc("灰度发布安排", "按内部用户、核心客户、全量用户三个阶段逐步放量。"),
                doc("发布风险清单", "按影响范围、触发条件、回滚动作和通知对象记录活动上线前必须确认的风险项。"),
                doc("客户灰度名单", "记录优先灰度客户、观察指标、联系人和问题升级路径，避免全量发布前缺少样本反馈。"),
                doc("活动数据看板口径", "统一曝光、点击、转化、阅读完成率、收藏率和反馈工单量的统计口径。"),
                doc("回滚演练记录", "记录最近一次回滚演练的步骤、耗时、负责人、发现问题和补救措施。"),
                doc("应急联系人清单", "沉淀运营、产品、研发、测试、运维和客服的应急联系人，便于演示私有知识库权限。"),
                doc("上线后观察指标", "上线后重点观察接口错误率、页面访问量、搜索命中率、用户反馈和知识库阅读量。")
        ));
    }

    private void assignDepartmentManagers(Map<String, Department> depts, Map<String, UserAccount> users) {
        manager(depts.get("company"), users.get("product_manager"));
        manager(depts.get("product"), users.get("product_manager"));
        manager(depts.get("frontend"), users.get("frontend_lead"));
        manager(depts.get("backend"), users.get("backend_lead"));
        manager(depts.get("qa"), users.get("qa_lead"));
        manager(depts.get("ops"), users.get("ops_engineer"));
        manager(depts.get("support"), users.get("support_agent"));
        manager(depts.get("knowledge"), users.get("knowledge_editor"));
        manager(depts.get("security"), users.get("security_admin"));
        manager(depts.get("search"), users.get("search_engineer"));

        UserAccount fallbackManager = users.get("product_manager");
        if (fallbackManager != null) {
            departmentRepository.findAll().stream()
                    .filter(department -> department.getDeletedAt() == null)
                    .filter(department -> department.getManagerId() == null)
                    .forEach(department -> manager(department, fallbackManager));
        }
    }

    private void manager(Department department, UserAccount user) {
        if (department == null || user == null) {
            return;
        }
        department.setManagerId(user.getId());
        departmentRepository.save(department);
    }

    private void cleanupGeneratedTestAccounts() {
        LocalDateTime now = LocalDateTime.now();
        for (UserAccount user : userRepository.findAll()) {
            String username = user.getUsername();
            if (username == null) {
                continue;
            }
            if (username.startsWith("smoke") || username.startsWith("e2e") || "newuser".equals(username)
                    || "tech_lead".equals(username) || "ops_specialist".equals(username)) {
                user.setEmail(null);
                user.setPhone(null);
                user.setDeletedAt(now);
                userRepository.save(user);
            }
        }
    }

    private Department department(String name, Department parent, String description) {
        Department dept = departmentRepository.findAll().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Department created = new Department();
                    created.setId(idGenerator.nextId());
                    created.setName(name);
                    return created;
                });
        dept.setParentId(parent == null ? null : parent.getId());
        dept.setDescription(description);
        dept.setDeletedAt(null);
        return departmentRepository.save(dept);
    }

    private UserAccount demoUser(String username, String nickname, String email, String phone, Department department) {
        UserAccount user = userRepository.findByUsername(username).orElseGet(() -> {
            UserAccount created = new UserAccount();
            created.setId(idGenerator.nextId());
            created.setUsername(username);
            created.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
            return created;
        });
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(UserRole.USER);
        user.setDepartmentId(department.getId());
        user.setDemoPassword(DEMO_PASSWORD);
        user.setDeletedAt(null);
        return userRepository.save(user);
    }

    private String keyToDept(String userKey) {
        return switch (userKey) {
            case "product_manager", "data_analyst" -> "product";
            case "ux_designer", "frontend_lead" -> "frontend";
            case "backend_lead", "java_engineer" -> "backend";
            case "qa_lead", "test_engineer" -> "qa";
            case "ops_engineer", "release_manager" -> "ops";
            case "support_agent", "customer_success" -> "support";
            case "knowledge_editor", "content_reviewer", "training_coach" -> "knowledge";
            case "security_admin", "audit_specialist" -> "security";
            case "search_engineer" -> "search";
            default -> "company";
        };
    }

    private void membership(UserAccount user, Department department) {
        UserTeamMembership membership = teamMembershipRepository
                .findByUserIdAndTeamIdAndDeletedAtIsNull(user.getId(), department.getId())
                .orElseGet(() -> {
                    UserTeamMembership created = new UserTeamMembership();
                    created.setId(idGenerator.nextId());
                    created.setUserId(user.getId());
                    created.setTeamId(department.getId());
                    return created;
                });
        membership.setDeletedAt(null);
        teamMembershipRepository.save(membership);
    }

    private KnowledgeBase knowledgeBase(String name, KnowledgeBaseType type, UserAccount owner, Department team, String description) {
        KnowledgeBase kb = knowledgeBaseRepository.findAll().stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseGet(() -> {
                    KnowledgeBase created = new KnowledgeBase();
                    created.setId(idGenerator.nextId());
                    created.setName(name);
                    return created;
                });
        kb.setType(type);
        kb.setOwnerId(owner.getId());
        kb.setTeamId(team == null ? null : team.getId());
        kb.setDescription(description);
        kb.setDeletedAt(null);
        return knowledgeBaseRepository.save(kb);
    }

    private void grant(KnowledgeBase kb, Map<String, UserAccount> users, Object... userRolePairs) {
        for (int i = 0; i < userRolePairs.length - 1; i += 2) {
            UserAccount user = users.get(String.valueOf(userRolePairs[i]));
            MemberRole role = (MemberRole) userRolePairs[i + 1];
            member(kb, user, role);
        }
    }

    private void member(KnowledgeBase kb, UserAccount user, MemberRole role) {
        KnowledgeBaseMember member = memberRepository.findByKbIdAndUserId(kb.getId(), user.getId())
                .orElseGet(() -> {
                    KnowledgeBaseMember created = new KnowledgeBaseMember();
                    created.setId(idGenerator.nextId());
                    created.setKbId(kb.getId());
                    created.setUserId(user.getId());
                    return created;
                });
        member.setRole(role);
        member.setDeletedAt(null);
        memberRepository.save(member);
    }

    private DocSeed doc(String title, String summary) {
        return new DocSeed(title, summary);
    }

    private void documents(KnowledgeBase kb, UserAccount owner, List<DocSeed> docs) {
        for (DocSeed doc : docs) {
            document(kb, owner, doc.title(), markdown(kb, doc));
        }
    }

    private String markdown(KnowledgeBase kb, DocSeed doc) {
        return """
                # %s

                ## 业务背景
                %s

                ## 适用范围
                本文档适用于「%s」相关成员，用于统一流程、标准和演示口径。

                ## 操作要点
                - 明确负责人、参与人和验收标准。
                - 关键变更需要在知识库中沉淀版本记录。
                - 涉及权限、发布或客户影响的事项必须保留审计线索。

                ## 演示建议
                展示时可以切换不同账号，观察知识库可见性、文档目录、评论收藏和后台审计数据。
                """.formatted(doc.title(), doc.summary(), kb.getName());
    }

    private WikiDocument document(KnowledgeBase kb, UserAccount owner, String title, String markdown) {
        WikiDocument doc = documentRepository.findByKbIdAndTitleStartingWithAndDeletedAtIsNull(kb.getId(), title)
                .stream()
                .filter(item -> title.equals(item.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    WikiDocument created = new WikiDocument();
                    created.setId(idGenerator.nextId());
                    created.setKbId(kb.getId());
                    created.setTitle(title);
                    created.setViewCount(0L);
                    created.setVersionNo(1);
                    return created;
                });
        String html = markdownService.toHtml(markdown);
        doc.setMarkdownContent(markdown);
        doc.setHtmlContent(html);
        doc.setOwnerId(owner.getId());
        doc.setVisibility(DocVisibility.PUBLIC);
        doc.setPublished(true);
        doc.setDeletedAt(null);
        WikiDocument saved = documentRepository.save(doc);
        storageService.savePublishedHtml(saved.getId(), html);
        return saved;
    }

    private record DocSeed(String title, String summary) {
    }
}

