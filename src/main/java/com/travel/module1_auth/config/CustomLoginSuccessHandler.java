package com.travel.module1_auth.config;

import com.travel.module1_auth.domain.AdminUserEntity;
import com.travel.module1_auth.domain.AuthEventType;
import com.travel.module1_auth.service.AdminUserService;
import com.travel.module1_auth.service.AuthAuditService;
import com.travel.module1_auth.service.SiteCustomerService;
import com.travel.common.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

/**
 * After a successful sign-in on /login:
 *  - travellers go back to the page that asked them to sign in (e.g. checkout), or to their profile;
 *  - staff go to the staff workspace (or to the password page after an admin-forced reset).
 */
@Component
public class CustomLoginSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final AuthAuditService auditService;
    private final AdminUserService adminUserService;
    private final SiteCustomerService siteCustomerService;
    private final int staffIdleMinutes;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public CustomLoginSuccessHandler(AuthAuditService auditService, AdminUserService adminUserService,
                                     SiteCustomerService siteCustomerService,
                                     @Value("${app.staff.session-timeout-minutes:30}") int staffIdleMinutes) {
        this.auditService = auditService;
        this.adminUserService = adminUserService;
        this.siteCustomerService = siteCustomerService;
        this.staffIdleMinutes = staffIdleMinutes;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        auditService.record(authentication.getName(), AuthEventType.LOGIN_SUCCESS, RequestUtils.clientIp(request), null);
        request.getSession().setMaxInactiveInterval(staffIdleMinutes * 60);

        SavedRequest saved = requestCache.getRequest(request, response);
        String savedPath = saved == null ? null : localPath(saved.getRedirectUrl());
        requestCache.removeRequest(request, response);

        if (SiteCustomerService.isMember(authentication)) {
            siteCustomerService.markLoggedIn(authentication.getName());
            String next = safeLocal(request.getParameter("continue"));
            if (next == null) next = savedPath;
            if (next == null || next.startsWith("/staff") || next.startsWith("/admin")) next = "/my";
            response.sendRedirect(next);
            return;
        }

        // After an admin-forced reset the user must choose a new password before anything else
        AdminUserEntity user = adminUserService.findByUsername(authentication.getName());
        if (user != null && Boolean.TRUE.equals(user.getMustChangePassword())) {
            response.sendRedirect("/account/password?forced");
            return;
        }
        boolean staffPage = savedPath != null && !savedPath.equals("/") && !savedPath.startsWith("/my")
                && !savedPath.startsWith("/checkout") && !savedPath.startsWith("/cart");
        response.sendRedirect(staffPage ? savedPath : "/staff");
    }

    /** Only same-site paths such as "/checkout" are accepted, never "//evil.com" or full URLs. */
    public static String safeLocal(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path.trim();
        if (!p.startsWith("/") || p.startsWith("//") || p.contains("\\") || p.contains("\r") || p.contains("\n")) return null;
        return p;
    }

    private static String localPath(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || path.startsWith("/error")) return null;
            String q = uri.getRawQuery();
            // Spring adds "continue" to the saved URL; drop it
            if (q != null) q = q.replaceAll("(^|&)continue(=[^&]*)?", "").replaceAll("^&", "");
            return safeLocal(path + (q == null || q.isEmpty() ? "" : "?" + q));
        } catch (Exception e) {
            return null;
        }
    }
}
