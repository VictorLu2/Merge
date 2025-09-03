package com.admin_log.audit;

import com.admin_log.entity.AdminLog;
import com.admin_log.service.AdminLogService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pagerealm.security.service.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminActionLoggingInterceptor implements HandlerInterceptor {

    private final AdminLogService adminLogService;
    // 允許從 JSON 解析欄位（title、name、code、nextStatus 等）
    private final ObjectMapper objectMapper;

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("__adminLogStart", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return;

            Object principal = auth.getPrincipal();
            if (!(principal instanceof UserDetailsImpl user)) return;

            String uri = request.getRequestURI();
            if (uri == null) return;

            // 僅處理 /api/admin/**
            if (!uri.startsWith("/api/admin/")) return;

            // 排除自身的管理日誌 API
            if (uri.startsWith("/api/admin/admin-logs")) return;

            String method = request.getMethod();

            // 僅記錄寫入類操作
            if (!WRITE_METHODS.contains(method)) return;

            // 僅記錄特定資源：書籍、優惠券、會員相關、客服工單
            boolean isTarget = false;
            String targetType = null;

            if (uri.startsWith("/api/admin/books")) {
                isTarget = true;
                targetType = "Book";
            } else if (uri.startsWith("/api/admin/coupons")) {
                isTarget = true;
                targetType = "Coupon";
            } else if (uri.startsWith("/api/admin/support")) {
                // 工單（回覆、狀態變更等）
                isTarget = true;
                targetType = "SupportTicket";
            } else if (uri.startsWith("/api/admin/update-enabled-status") || uri.startsWith("/api/admin/add-user")) {
                isTarget = true;
                targetType = "User";
            }

            if (!isTarget) return; // 其他資源不記錄

            String actionZh = switch (method) {
                case "POST" -> "建立";
                case "PUT", "PATCH" -> "更新";
                case "DELETE" -> "刪除";
                default -> method;
            };

            Long targetId = extractTargetId(request, uri);

            // 若 handler 可用，保持原行為推導類名（作為備援）
            if (targetType == null && handler instanceof HandlerMethod hm) {
                targetType = hm.getBeanType().getSimpleName();
            }

            // 將 targetType 儲存為中文
            String targetTypeZh = toZhTargetType(targetType);

            long durationMs = 0L;
            Object start = request.getAttribute("__adminLogStart");
            if (start instanceof Long s) {
                durationMs = System.currentTimeMillis() - s;
            }

            String query = request.getQueryString();

            // 從業務層可選標誌讀取成功/訊息（控制器可自行設定 request.setAttribute("__bizSuccess", true/false) 與 "__bizMessage"）
            Boolean bizSuccessAttr = safeBooleanAttr(request.getAttribute("__bizSuccess"));
            String bizMessage = safeString(request.getAttribute("__bizMessage"));

            boolean success = bizSuccessAttr != null ? bizSuccessAttr : ((response.getStatus() >= 200 && response.getStatus() < 300) && ex == null);

            // 若為失敗案例則不記錄
            if (!success) return;

            String zhSummary = buildZhSummary(method, targetType, targetId, request);
            String ip = getClientIp(request);
            String ua = safeHeader(request, "User-Agent");
            String rawUri = uri + (query != null ? ("?" + query) : "");

            String details = "【" + (success ? "成功" : "失敗") + "】" + zhSummary +
                    (bizMessage.isEmpty() ? "" : ("；訊息=" + truncate(bizMessage, 200))) +
                    "；狀態碼=" + response.getStatus() +
                    "；耗時=" + durationMs + "ms" +
                    "；IP=" + ip +
                    "；UA=" + truncate(ua, 200) +
                    "。原始: method=" + method + ", uri=" + rawUri +
                    (ex != null ? (", error=" + ex.getClass().getSimpleName() + ":" + truncate(ex.getMessage(), 200)) : "");

            AdminLog log = AdminLog.builder()
                    .adminId(user.getId())
                    .action(actionZh) // 儲存中文動作：建立/更新/刪除
                    .targetId(targetId)
                    .targetType(targetTypeZh) // 儲存中文對象：書籍/優惠券/會員/工單
                    .details(details)
                    .build();

            adminLogService.create(log);
        } catch (Exception ignored) {
            // 任何例外不影響原請求
        }
    }

    private Long extractTargetId(HttpServletRequest request, String uri) {
        Long targetId = null;

        // 1) PathVariables: id, targetId, userId, bookId, couponId
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (attr instanceof Map<?, ?> vars) {
            for (String key : new String[]{"id", "targetId", "userId", "bookId", "couponId"}) {
                Object v = vars.get(key);
                if (v != null) {
                    try { targetId = Long.valueOf(v.toString()); } catch (NumberFormatException ignored) {}
                    if (targetId != null) return targetId;
                }
            }
        }

        // 2) Query string 或表單參數（例如 update-enabled-status?userId=1）
        for (String key : new String[]{"id", "userId", "bookId", "couponId"}) {
            String v = request.getParameter(key);
            if (v != null) {
                try { targetId = Long.valueOf(v); } catch (NumberFormatException ignored) {}
                if (targetId != null) return targetId;
            }
        }

        return null;
    }

    private String buildZhSummary(String method, String targetType, Long targetId, HttpServletRequest request) {
        String entityZh = switch (targetType == null ? "" : targetType) {
            case "Book" -> "書籍";
            case "Coupon" -> "優惠券";
            case "User" -> "會員";
            case "SupportTicket" -> "工單";
            default -> (targetType != null ? targetType : "資源");
        };
        String verbZh = switch (method) {
            case "POST" -> "建立";
            case "PUT", "PATCH" -> "更新";
            case "DELETE" -> "刪除";
            default -> method;
        };

        // 嘗試擷取可讀關鍵欄位（優先 Query/Form，其次 JSON Body）
        String title = nvl(request.getParameter("title"));
        String code = nvl(request.getParameter("code"));
        String name = nvl(request.getParameter("name"));
        String enabled = nvl(request.getParameter("enabled"));
        String nextStatus = nvl(request.getParameter("nextStatus"));
        String content = nvl(request.getParameter("content"));

        if (isEmpty(title) || isEmpty(name) || isEmpty(code) || isEmpty(nextStatus) || isEmpty(content)) {
            Map<String, Object> bodyMap = readJsonBodyMap(request);
            if (isEmpty(title)) title = strVal(bodyMap.get("title"));
            if (isEmpty(name)) name = strVal(bodyMap.get("name"));
            if (isEmpty(code)) code = strVal(bodyMap.get("code"));
            // 若 body 有 genericCode，當作 code 補值
            if (isEmpty(code)) code = strVal(bodyMap.get("genericCode"));
            if (isEmpty(nextStatus)) nextStatus = strVal(bodyMap.get("nextStatus"));
            if (isEmpty(content)) content = strVal(bodyMap.get("content"));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(verbZh).append(entityZh);
        if (targetId != null) sb.append("(ID=").append(targetId).append(")");
        if (!title.isEmpty()) sb.append("，標題=").append(truncate(title, 50));
        if (!code.isEmpty()) sb.append("，代碼=").append(truncate(code, 50));
        if (!name.isEmpty()) sb.append("，名稱=").append(truncate(name, 50));
        if (!enabled.isEmpty()) sb.append("，啟用=").append(enabled);
        if (!nextStatus.isEmpty()) sb.append("，下一狀態=").append(nextStatus);
        if (!content.isEmpty() && "工單".equals(entityZh)) sb.append("，回覆=").append(truncate(content, 50));
        return sb.toString();
    }

    private Map<String, Object> readJsonBodyMap(HttpServletRequest request) {
        try {
            if (request instanceof ContentCachingRequestWrapper wrapper) {
                byte[] buf = wrapper.getContentAsByteArray();
                if (buf.length == 0) return Map.of();
                String body = new String(buf, StandardCharsets.UTF_8).trim();
                if (body.isEmpty() || body.startsWith("[") || !body.startsWith("{")) return Map.of();
                return objectMapper.readValue(body, new TypeReference<Map<String, Object>>(){});
            }
        } catch (Exception ignored) {}
        return Map.of();
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank()) {
                int comma = v.indexOf(',');
                return comma > 0 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String safeHeader(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null) return "";
        return v.length() > 200 ? v.substring(0, 200) : v;
    }

    private Boolean safeBooleanAttr(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        return null;
    }

    private String safeString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private boolean isEmpty(String s) { return s == null || s.isEmpty(); }

    private String nvl(String s) { return s == null ? "" : s; }

    private String strVal(Object o) { return o == null ? "" : String.valueOf(o); }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String toZhTargetType(String targetType) {
        return switch (targetType == null ? "" : targetType) {
            case "Book" -> "書籍";
            case "Coupon" -> "優惠券";
            case "User" -> "會員";
            case "SupportTicket" -> "工單";
            default -> (targetType != null ? targetType : "資源");
        };
    }
}
