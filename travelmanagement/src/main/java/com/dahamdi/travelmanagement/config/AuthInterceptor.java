package com.dahamdi.travelmanagement.config;

import com.dahamdi.travelmanagement.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * Runs before EVERY request (except login/register/static files - see WebConfig).
 *  - Not logged in            -> pages redirect to /login, API returns 401
 *  - Logged in but not ADMIN  -> admin pages redirect to the user dashboard, API returns 403
 *
 * Admin-only areas: /admin/**, /invoices/**, /payments/**, /api/**
 * except /api/my/** (a logged-in user's own data). /api/auth/** is public (see WebConfig).
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER = "loggedInUser";

    private static final String[] ADMIN_ONLY_PREFIXES = {"/admin", "/invoices", "/payments", "/api"};

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean isApi = path.startsWith("/api/") || path.equals("/api")
                || path.equals("/payments/gateway-pay");

        HttpSession session = request.getSession(false);
        User user = session != null ? (User) session.getAttribute(SESSION_USER) : null;

        if (user == null) {
            if (isApi) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Please log in first.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return false;
        }

        if (isAdminOnly(path) && !"ADMIN".equals(user.getRole())) {
            if (isApi) {
                sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Admin access required.");
            } else {
                response.sendRedirect(request.getContextPath() + "/user/dashboard");
            }
            return false;
        }

        return true;
    }

    private static boolean isAdminOnly(String path) {
        if (path.equals("/api/my") || path.startsWith("/api/my/")) {
            return false;
        }
        for (String prefix : ADMIN_ONLY_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\",\"message\":\"" + message + "\"}");
    }
}
