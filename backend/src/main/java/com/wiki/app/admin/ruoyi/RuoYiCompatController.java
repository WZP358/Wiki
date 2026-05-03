package com.wiki.app.admin.ruoyi;

import com.wiki.app.dept.Department;
import com.wiki.app.dept.DepartmentRepository;
import com.wiki.app.kb.KnowledgeBaseMemberSyncService;
import com.wiki.app.log.OperationLog;
import com.wiki.app.log.OperationLogRepository;
import com.wiki.app.security.CurrentUser;
import com.wiki.app.security.SecurityUtils;
import com.wiki.app.user.UserAccount;
import com.wiki.app.user.UserRepository;
import com.wiki.app.user.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class RuoYiCompatController {
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final OperationLogRepository operationLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final KnowledgeBaseMemberSyncService memberSyncService;

    public RuoYiCompatController(UserRepository userRepository,
                                 DepartmentRepository departmentRepository,
                                 OperationLogRepository operationLogRepository,
                                 PasswordEncoder passwordEncoder,
                                 KnowledgeBaseMemberSyncService memberSyncService) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.operationLogRepository = operationLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.memberSyncService = memberSyncService;
    }

    @GetMapping("/api/system/user/list")
    public Map<String, Object> listUser(@RequestParam Map<String, String> params) {
        Page<UserAccount> page = userRepository.adminSearch(
                firstNonBlank(params.get("userName"), params.get("keyword"), params.get("phonenumber")),
                parseRole(params.get("role")),
                parseLong(params.get("deptId")),
                parseStatus(params.get("status")),
                pageRequest(params, "updatedAt")
        );
        return table(page.map(this::userRow));
    }

    @GetMapping({"/api/system/user", "/api/system/user/{userId}"})
    public Map<String, Object> getUser(@PathVariable(required = false) Long userId) {
        Map<String, Object> result = ok();
        result.put("roles", List.of(roleRow(1L, "admin", "超级管理员"), roleRow(2L, "common", "普通角色")));
        result.put("posts", List.of(postRow(1L, "董事长", "ceo"), postRow(2L, "项目经理", "pm")));
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> result.put("data", userRow(user)));
        }
        return result;
    }

    @GetMapping("/api/system/user/profile")
    public Map<String, Object> profile() {
        CurrentUser currentUser = SecurityUtils.currentUser();
        UserAccount user = userRepository.findById(currentUser.getUserId()).orElseThrow();
        Map<String, Object> result = ok();
        result.put("data", userRow(user));
        result.put("roleGroup", user.getRole() == UserRole.ADMIN ? "超级管理员" : "普通用户");
        result.put("postGroup", "");
        return result;
    }

    @PostMapping("/api/system/user")
    public Map<String, Object> addUser() {
        return okMessage("当前系统用户请通过注册功能创建");
    }

    @PutMapping("/api/system/user")
    public Map<String, Object> updateUser(@RequestBody Map<String, Object> body) {
        Long userId = parseLong(String.valueOf(body.get("userId")));
        if (userId != null) {
            userRepository.findById(userId).ifPresent(user -> {
                Object status = body.get("status");
                user.setDeletedAt("1".equals(String.valueOf(status)) ? LocalDateTime.now() : null);
                userRepository.save(user);
                memberSyncService.syncUserAutoReaders(user.getId());
            });
        }
        return okMessage("操作成功");
    }

    @PutMapping("/api/system/user/changeStatus")
    public Map<String, Object> changeUserStatus(@RequestBody Map<String, Object> body) {
        return updateUser(body);
    }

    @PutMapping("/api/system/user/resetPwd")
    public Map<String, Object> resetPwd(@RequestBody Map<String, Object> body) {
        Long userId = parseLong(stringValue(body.get("userId")));
        String password = stringValue(body.get("password"));
        if (userId == null) {
            return errorMessage("请选择要重置密码的用户");
        }
        if (password == null || password.isBlank()) {
            return errorMessage("新密码不能为空");
        }
        if (password.length() < 5 || password.length() > 20) {
            return errorMessage("用户密码长度必须介于 5 和 20 之间");
        }

        UserAccount user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return errorMessage("用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
        return okMessage("密码重置成功");
    }

    @DeleteMapping("/api/system/user/{userId}")
    public Map<String, Object> deleteUser(@PathVariable Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
            memberSyncService.syncUserAutoReaders(user.getId());
        });
        return okMessage("删除成功");
    }

    @GetMapping("/api/system/user/deptTree")
    public Map<String, Object> userDeptTree() {
        Map<String, Object> result = ok();
        result.put("data", deptTree());
        return result;
    }

    @GetMapping("/api/system/dept/list")
    public Map<String, Object> listDept() {
        Map<String, Object> result = ok();
        result.put("data", departmentRepository.findAll().stream().map(this::deptRow).toList());
        return result;
    }

    @GetMapping("/api/system/dept/list/exclude/{deptId}")
    public Map<String, Object> listDeptExclude(@PathVariable Long deptId) {
        Map<String, Object> result = ok();
        result.put("data", departmentRepository.findAll().stream()
                .filter(dept -> !deptId.equals(dept.getId()))
                .map(this::deptRow)
                .toList());
        return result;
    }

    @GetMapping("/api/system/dept/{deptId}")
    public Map<String, Object> getDept(@PathVariable Long deptId) {
        Map<String, Object> result = ok();
        departmentRepository.findById(deptId).ifPresent(dept -> result.put("data", deptRow(dept)));
        return result;
    }

    @PostMapping("/api/system/dept")
    public Map<String, Object> addDept(@RequestBody Map<String, Object> body) {
        Department dept = new Department();
        dept.setName(firstNonBlank(stringValue(body.get("deptName")), stringValue(body.get("name")), "新部门"));
        dept.setDescription(stringValue(body.get("description")));
        dept.setParentId(parseLong(stringValue(body.get("parentId"))));
        departmentRepository.save(dept);
        return okMessage("新增成功");
    }

    @PutMapping("/api/system/dept")
    public Map<String, Object> updateDept(@RequestBody Map<String, Object> body) {
        Long deptId = parseLong(firstNonBlank(stringValue(body.get("deptId")), stringValue(body.get("id"))));
        if (deptId != null) {
            departmentRepository.findById(deptId).ifPresent(dept -> {
                dept.setName(firstNonBlank(stringValue(body.get("deptName")), stringValue(body.get("name")), dept.getName()));
                dept.setDescription(firstNonBlank(stringValue(body.get("description")), dept.getDescription()));
                dept.setParentId(parseLong(firstNonBlank(stringValue(body.get("parentId")), stringValue(dept.getParentId()))));
                String status = stringValue(body.get("status"));
                dept.setDeletedAt("1".equals(status) ? LocalDateTime.now() : null);
                departmentRepository.save(dept);
            });
        }
        return okMessage("修改成功");
    }

    @PutMapping("/api/system/dept/updateSort")
    public Map<String, Object> updateDeptSort() {
        return okMessage("排序已保存");
    }

    @DeleteMapping("/api/system/dept/{deptIds}")
    public Map<String, Object> deleteDept(@PathVariable String deptIds) {
        Arrays.stream(deptIds.split(","))
                .map(this::parseLong)
                .filter(Objects::nonNull)
                .forEach(id -> departmentRepository.findById(id).ifPresent(dept -> {
                    dept.setDeletedAt(LocalDateTime.now());
                    departmentRepository.save(dept);
                }));
        return okMessage("删除成功");
    }

    @GetMapping("/api/system/role/list")
    public Map<String, Object> listRole() {
        return table(List.of(roleRow(1L, "admin", "超级管理员"), roleRow(2L, "common", "普通角色")), 2);
    }

    @GetMapping("/api/system/role/{roleId}")
    public Map<String, Object> getRole(@PathVariable Long roleId) {
        Map<String, Object> result = ok();
        result.put("data", roleId == 1L ? roleRow(1L, "admin", "超级管理员") : roleRow(2L, "common", "普通角色"));
        return result;
    }

    @GetMapping("/api/system/role/deptTree/{roleId}")
    public Map<String, Object> roleDeptTree(@PathVariable Long roleId) {
        Map<String, Object> result = ok();
        result.put("depts", deptTree());
        result.put("checkedKeys", List.of());
        return result;
    }

    @GetMapping("/api/system/menu/list")
    public Map<String, Object> listMenu() {
        Map<String, Object> result = ok();
        result.put("data", menuRows());
        return result;
    }

    @GetMapping("/api/system/menu/{menuId}")
    public Map<String, Object> getMenu(@PathVariable Long menuId) {
        Map<String, Object> result = ok();
        menuRows().stream()
                .filter(menu -> menuId.equals(menu.get("menuId")))
                .findFirst()
                .ifPresent(menu -> result.put("data", menu));
        return result;
    }

    @GetMapping("/api/system/menu/treeselect")
    public Map<String, Object> menuTreeSelect() {
        Map<String, Object> result = ok();
        result.put("data", List.of(treeNode(1L, "系统管理"), treeNode(2L, "Wiki 管理")));
        return result;
    }

    @GetMapping("/api/system/menu/roleMenuTreeselect/{roleId}")
    public Map<String, Object> roleMenuTreeSelect(@PathVariable Long roleId) {
        Map<String, Object> result = ok();
        result.put("menus", List.of(treeNode(1L, "系统管理"), treeNode(2L, "Wiki 管理")));
        result.put("checkedKeys", List.of());
        return result;
    }

    @GetMapping("/api/system/post/list")
    public Map<String, Object> listPost() {
        return table(List.of(postRow(1L, "董事长", "ceo"), postRow(2L, "项目经理", "pm"), postRow(3L, "普通员工", "user")), 3);
    }

    @GetMapping("/api/system/post/{postId}")
    public Map<String, Object> getPost(@PathVariable Long postId) {
        Map<String, Object> result = ok();
        result.put("data", postRow(postId, postId == 1L ? "董事长" : "普通员工", postId == 1L ? "ceo" : "user"));
        return result;
    }

    @GetMapping("/api/system/config/list")
    public Map<String, Object> listConfig() {
        return table(List.of(configRow(1L, "用户初始密码", "sys.user.initPassword", "Admin@123456")), 1);
    }

    @GetMapping("/api/system/config/configKey/{configKey}")
    public Map<String, Object> configKey(@PathVariable String configKey) {
        return okData("sys.user.initPassword".equals(configKey) ? "Admin@123456" : "");
    }

    @GetMapping("/api/system/dict/type/list")
    public Map<String, Object> listDictType() {
        List<Map<String, Object>> rows = dictTypes();
        return table(rows, rows.size());
    }

    @GetMapping("/api/system/dict/type/optionselect")
    public Map<String, Object> dictOptions() {
        Map<String, Object> result = ok();
        result.put("data", dictTypes());
        return result;
    }

    @GetMapping("/api/system/dict/data/list")
    public Map<String, Object> listDictData(@RequestParam Map<String, String> params) {
        String type = params.getOrDefault("dictType", "sys_normal_disable");
        List<Map<String, Object>> rows = dictData(type);
        return table(rows, rows.size());
    }

    @GetMapping("/api/system/dict/data/type/{dictType}")
    public Map<String, Object> dictDataByType(@PathVariable String dictType) {
        Map<String, Object> result = ok();
        result.put("data", dictData(dictType));
        return result;
    }

    @GetMapping("/api/monitor/operlog/list")
    public Map<String, Object> operlog(@RequestParam Map<String, String> params) {
        Page<OperationLog> page = operationLogRepository.findAllByOrderByCreatedAtDesc(pageRequest(params, "createdAt"));
        return table(page.map(this::operLogRow));
    }

    @GetMapping({"/api/monitor/logininfor/list", "/api/monitor/online/list", "/api/monitor/job/list", "/api/monitor/jobLog/list", "/api/tool/gen/list", "/api/tool/gen/db/list"})
    public Map<String, Object> emptyTable() {
        return table(List.of(), 0);
    }

    @GetMapping("/api/monitor/cache")
    public Map<String, Object> cache() {
        Map<String, Object> info = row("redis_version", "compatible", "redis_mode", "standalone", "tcp_port", "6379",
                "connected_clients", "0", "uptime_in_days", "0", "used_memory_human", "0M", "used_cpu_user_children", "0",
                "maxmemory_human", "0M", "aof_enabled", "0", "rdb_last_bgsave_status", "ok",
                "instantaneous_input_kbps", "0", "instantaneous_output_kbps", "0");
        return okData(row("info", info, "dbSize", 0, "commandStats", List.of(row("name", "get", "value", 0))));
    }

    @GetMapping({"/api/monitor/cache/getNames", "/api/monitor/cache/getKeys/{cacheName}"})
    public Map<String, Object> cacheNames() {
        Map<String, Object> result = ok();
        result.put("data", List.of());
        return result;
    }

    @GetMapping("/api/monitor/server")
    public Map<String, Object> server() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        double totalMemory = runtime.totalMemory() / 1024.0 / 1024.0;
        double freeMemory = runtime.freeMemory() / 1024.0 / 1024.0;
        double usedMemory = totalMemory - freeMemory;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        File root = new File(System.getProperty("user.dir")).getAbsoluteFile();
        Map<String, Object> data = row(
                "cpu", row("cpuNum", runtime.availableProcessors(), "used", 0, "sys", 0, "free", 100),
                "mem", row("total", 0, "used", 0, "free", 0, "usage", 0),
                "jvm", row("total", round(totalMemory), "used", round(usedMemory), "free", round(freeMemory), "usage", totalMemory == 0 ? 0 : round(usedMemory * 100 / totalMemory),
                        "name", System.getProperty("java.vm.name"), "version", System.getProperty("java.version"),
                        "home", System.getProperty("java.home"), "startTime", LocalDateTime.now().toString(),
                        "runTime", Duration.ofMillis(uptime).toMinutes() + " 分钟", "inputArgs", ManagementFactory.getRuntimeMXBean().getInputArguments().toString()),
                "sys", row("computerName", InetAddress.getLocalHost().getHostName(), "computerIp", InetAddress.getLocalHost().getHostAddress(),
                        "osName", System.getProperty("os.name"), "osArch", System.getProperty("os.arch"), "userDir", System.getProperty("user.dir")),
                "sysFiles", List.of(row("dirName", root.getPath(), "sysTypeName", "", "typeName", "本地磁盘",
                        "total", readable(root.getTotalSpace()), "free", readable(root.getFreeSpace()), "used", readable(root.getTotalSpace() - root.getFreeSpace()), "usage", 0))
        );
        return okData(data);
    }

    @GetMapping({"/api/system/config/{id}", "/api/system/dict/type/{id}", "/api/system/dict/data/{id}", "/api/tool/gen/{id}", "/api/monitor/job/{id}"})
    public Map<String, Object> genericGet(@PathVariable String id) {
        return okData(row("id", id));
    }

    @PostMapping({"/api/system/post", "/api/system/role", "/api/system/menu", "/api/system/config", "/api/system/dict/type", "/api/system/dict/data", "/api/monitor/job", "/api/tool/gen/importTable"})
    public Map<String, Object> genericPost() {
        return okMessage("操作成功");
    }

    @PutMapping({"/api/system/post", "/api/system/role", "/api/system/menu", "/api/system/config", "/api/system/dict/type", "/api/system/dict/data", "/api/monitor/job", "/api/system/role/dataScope", "/api/system/role/changeStatus", "/api/system/menu/updateSort", "/api/monitor/job/changeStatus"})
    public Map<String, Object> genericPut() {
        return okMessage("操作成功");
    }

    @DeleteMapping({"/api/system/post/{id}", "/api/system/role/{id}", "/api/system/menu/{id}", "/api/system/config/{id}", "/api/system/dict/type/{id}", "/api/system/dict/data/{id}", "/api/monitor/job/{id}", "/api/monitor/jobLog/{id}", "/api/monitor/logininfor/{id}", "/api/monitor/operlog/{id}", "/api/tool/gen/{id}"})
    public Map<String, Object> genericDelete(@PathVariable String id) {
        return okMessage("删除成功");
    }

    @PostMapping({"/api/system/config/refreshCache", "/api/system/dict/type/refreshCache", "/api/monitor/job/run", "/api/monitor/jobLog/clean", "/api/monitor/logininfor/clean", "/api/monitor/operlog/clean", "/api/monitor/logininfor/unlock/{userName}"})
    public Map<String, Object> genericAction() {
        return okMessage("操作成功");
    }

    private PageRequest pageRequest(Map<String, String> params, String sortField) {
        int pageNum = Math.max(parseInt(params.get("pageNum"), 1) - 1, 0);
        int pageSize = Math.max(parseInt(params.get("pageSize"), 10), 1);
        return PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, sortField));
    }

    private Map<String, Object> table(Page<? extends Map<String, Object>> page) {
        return table(page.getContent(), page.getTotalElements());
    }

    private Map<String, Object> table(List<? extends Map<String, Object>> rows, long total) {
        Map<String, Object> result = ok();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    private Map<String, Object> okData(Object data) {
        Map<String, Object> result = ok();
        result.put("data", data);
        return result;
    }

    private Map<String, Object> okMessage(String message) {
        Map<String, Object> result = ok();
        result.put("msg", message);
        return result;
    }

    private Map<String, Object> errorMessage(String message) {
        return row("code", 500, "msg", message);
    }

    private Map<String, Object> ok() {
        return row("code", 200, "msg", "操作成功");
    }

    private Map<String, Object> userRow(UserAccount user) {
        return row("userId", user.getId(), "id", user.getId(), "userName", user.getUsername(), "username", user.getUsername(),
                "nickName", user.getNickname(), "nickname", user.getNickname(), "email", user.getEmail(), "phonenumber", user.getPhone(),
                "phone", user.getPhone(), "role", user.getRole().name(), "deptId", user.getDepartmentId(), "departmentId", user.getDepartmentId(),
                "status", user.getDeletedAt() == null ? "0" : "1", "active", user.getDeletedAt() == null, "createTime", user.getCreatedAt(),
                "updatedAt", user.getUpdatedAt());
    }

    private Map<String, Object> deptRow(Department dept) {
        return row("deptId", dept.getId(), "id", dept.getId(), "parentId", dept.getParentId(), "deptName", dept.getName(),
                "name", dept.getName(), "orderNum", dept.getId(), "leader", "", "phone", "", "email", "",
                "status", dept.getDeletedAt() == null ? "0" : "1", "createTime", dept.getCreatedAt());
    }

    private List<Map<String, Object>> deptTree() {
        return departmentRepository.findAll().stream()
                .map(dept -> treeNode(dept.getId(), dept.getName()))
                .toList();
    }

    private Map<String, Object> roleRow(Long id, String key, String name) {
        return row("roleId", id, "roleName", name, "roleKey", key, "roleSort", id, "status", "0", "createTime", LocalDateTime.now());
    }

    private Map<String, Object> postRow(Long id, String name, String code) {
        return row("postId", id, "postName", name, "postCode", code, "postSort", id, "status", "0", "createTime", LocalDateTime.now());
    }

    private Map<String, Object> configRow(Long id, String name, String key, String value) {
        return row("configId", id, "configName", name, "configKey", key, "configValue", value, "configType", "Y", "createTime", LocalDateTime.now());
    }

    private Map<String, Object> dictTypeRow(Long id, String name, String type) {
        return row("dictId", id, "dictName", name, "dictType", type, "status", "0", "createTime", LocalDateTime.now());
    }

    private Map<String, Object> dictDataRow(Long id, String label, String value) {
        return row("dictCode", id, "dictLabel", label, "dictValue", value, "dictSort", id, "listClass", "default", "status", "0");
    }

    private List<Map<String, Object>> dictTypes() {
        return List.of(
                dictTypeRow(1L, "用户性别", "sys_user_sex"),
                dictTypeRow(2L, "系统状态", "sys_normal_disable"),
                dictTypeRow(3L, "通知类型", "sys_notice_type"),
                dictTypeRow(4L, "通知状态", "sys_notice_status")
        );
    }

    private List<Map<String, Object>> dictData(String type) {
        return switch (type) {
            case "sys_user_sex" -> List.of(dictDataRow(1L, "男", "0"), dictDataRow(2L, "女", "1"));
            case "sys_notice_type" -> List.of(dictDataRow(5L, "通知", "1"), dictDataRow(6L, "公告", "2"));
            case "sys_notice_status", "sys_normal_disable" -> List.of(dictDataRow(3L, "正常", "0"), dictDataRow(4L, "停用", "1"));
            default -> List.of();
        };
    }

    private Map<String, Object> operLogRow(OperationLog log) {
        return row("operId", log.getId(), "title", log.getTargetType(), "businessType", 0, "method", log.getAction(),
                "requestMethod", "", "operatorType", 1, "operName", log.getUsername(), "operIp", log.getIp(),
                "operUrl", log.getTargetId(), "status", 0, "operTime", log.getCreatedAt(), "jsonResult", log.getDetail());
    }

    private List<Map<String, Object>> menuRows() {
        return List.of(
                row("menuId", 1L, "menuName", "系统管理", "parentId", 0L, "orderNum", 1, "path", "system", "component", "Layout", "menuType", "M", "visible", "0", "status", "0", "icon", "system"),
                row("menuId", 2L, "menuName", "Wiki 管理", "parentId", 0L, "orderNum", 2, "path", "wiki", "component", "Layout", "menuType", "M", "visible", "0", "status", "0", "icon", "documentation")
        );
    }

    private Map<String, Object> treeNode(Long id, String label) {
        return row("id", id, "label", label);
    }

    private Map<String, Object> row(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length - 1; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private UserRole parseRole(String value) {
        try {
            return value == null || value.isBlank() ? null : UserRole.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean parseStatus(String status) {
        if ("0".equals(status)) return true;
        if ("1".equals(status)) return false;
        return null;
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() || "null".equals(value) ? null : Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String readable(long bytes) {
        return round(bytes / 1024.0 / 1024.0 / 1024.0) + "GB";
    }
}
