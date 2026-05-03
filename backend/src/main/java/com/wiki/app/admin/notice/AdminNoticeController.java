package com.wiki.app.admin.notice;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/system/notice")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {
    private static final AtomicLong ID = new AtomicLong(2);
    private static final List<Map<String, Object>> NOTICES = new ArrayList<>();

    static {
        NOTICES.add(row(
                "noticeId", 1L,
                "noticeTitle", "Wiki 后台管理已接入",
                "noticeType", "2",
                "noticeContent", "<p>后台已按若依风格收敛菜单，并保留当前项目可支撑的管理功能。</p>",
                "status", "0",
                "createBy", "admin",
                "createTime", LocalDateTime.now().toString(),
                "isRead", false
        ));
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Map<String, String> params) {
        String title = params.getOrDefault("noticeTitle", "").trim();
        String type = params.getOrDefault("noticeType", "").trim();
        List<Map<String, Object>> filtered = NOTICES.stream()
                .filter(item -> title.isBlank() || String.valueOf(item.get("noticeTitle")).contains(title))
                .filter(item -> type.isBlank() || type.equals(String.valueOf(item.get("noticeType"))))
                .sorted(Comparator.comparing(item -> String.valueOf(((Map<String, Object>) item).get("createTime"))).reversed())
                .collect(Collectors.toList());
        return table(page(filtered, params), filtered.size());
    }

    @GetMapping("/listTop")
    public Map<String, Object> listTop() {
        List<Map<String, Object>> rows = NOTICES.stream()
                .filter(item -> "0".equals(String.valueOf(item.getOrDefault("status", "0"))))
                .limit(5)
                .toList();
        return okData(row(
                "data", rows,
                "unreadCount", rows.stream().filter(item -> Boolean.FALSE.equals(item.get("isRead"))).count()
        ));
    }

    @GetMapping("/{noticeId}")
    public Map<String, Object> getNotice(@PathVariable Long noticeId) {
        return okData(NOTICES.stream()
                .filter(item -> noticeId.equals(item.get("noticeId")))
                .findFirst()
                .orElseGet(() -> row(
                        "noticeId", noticeId,
                        "noticeTitle", "系统公告",
                        "noticeType", "2",
                        "noticeContent", "<p>暂无公告详情。</p>",
                        "status", "0",
                        "createBy", "admin",
                        "createTime", LocalDateTime.now().toString(),
                        "isRead", true
                )));
    }

    @PostMapping
    public Map<String, Object> add(@RequestBody Map<String, Object> body) {
        Map<String, Object> item = new LinkedHashMap<>(body);
        item.put("noticeId", ID.getAndIncrement());
        item.putIfAbsent("noticeType", "2");
        item.putIfAbsent("status", "0");
        item.putIfAbsent("createBy", "admin");
        item.putIfAbsent("createTime", LocalDateTime.now().toString());
        item.putIfAbsent("isRead", false);
        NOTICES.add(item);
        return okMessage("新增成功");
    }

    @PutMapping
    public Map<String, Object> update(@RequestBody Map<String, Object> body) {
        Long noticeId = parseLong(body.get("noticeId"));
        if (noticeId == null) {
            return fail("公告编号不能为空");
        }
        for (int i = 0; i < NOTICES.size(); i++) {
            if (noticeId.equals(NOTICES.get(i).get("noticeId"))) {
                Map<String, Object> item = new LinkedHashMap<>(NOTICES.get(i));
                item.putAll(body);
                item.put("noticeId", noticeId);
                NOTICES.set(i, item);
                return okMessage("修改成功");
            }
        }
        return fail("公告不存在");
    }

    @DeleteMapping("/{noticeIds}")
    public Map<String, Object> delete(@PathVariable String noticeIds) {
        List<Long> ids = List.of(noticeIds.split(",")).stream()
                .map(AdminNoticeController::parseLong)
                .filter(id -> id != null)
                .toList();
        NOTICES.removeIf(item -> ids.contains(item.get("noticeId")));
        return okMessage("删除成功");
    }

    @PostMapping("/markRead")
    public Map<String, Object> markRead(@RequestParam(required = false) Long noticeId) {
        NOTICES.stream()
                .filter(item -> noticeId == null || noticeId.equals(item.get("noticeId")))
                .forEach(item -> item.put("isRead", true));
        return okMessage("操作成功");
    }

    @PostMapping("/markReadAll")
    public Map<String, Object> markReadAll(@RequestParam(required = false) String ids) {
        List<Long> targetIds = ids == null || ids.isBlank()
                ? List.of()
                : List.of(ids.split(",")).stream().map(AdminNoticeController::parseLong).filter(id -> id != null).toList();
        NOTICES.stream()
                .filter(item -> targetIds.isEmpty() || targetIds.contains(item.get("noticeId")))
                .forEach(item -> item.put("isRead", true));
        return okMessage("操作成功");
    }

    private static List<Map<String, Object>> page(List<Map<String, Object>> rows, Map<String, String> params) {
        int pageNum = Math.max(parseInt(params.get("pageNum"), 1), 1);
        int pageSize = Math.max(parseInt(params.get("pageSize"), 10), 1);
        int from = Math.min((pageNum - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        return rows.subList(from, to);
    }

    private static Map<String, Object> table(List<Map<String, Object>> rows, long total) {
        Map<String, Object> result = ok();
        result.put("rows", rows);
        result.put("total", total);
        return result;
    }

    private static Map<String, Object> okData(Object data) {
        Map<String, Object> result = ok();
        result.put("data", data);
        return result;
    }

    private static Map<String, Object> okMessage(String message) {
        Map<String, Object> result = ok();
        result.put("msg", message);
        return result;
    }

    private static Map<String, Object> fail(String message) {
        return row("code", 500, "msg", message);
    }

    private static Map<String, Object> ok() {
        return row("code", 200, "msg", "操作成功");
    }

    private static Map<String, Object> row(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length - 1; i += 2) {
            map.put(String.valueOf(values[i]), values[i + 1]);
        }
        return map;
    }

    private static Long parseLong(Object value) {
        try {
            return value == null || String.valueOf(value).isBlank() ? null : Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
