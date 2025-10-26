package xmate.com.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleRedirectSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth) throws IOException {

        boolean isStaff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null)
                .anyMatch(a -> a.startsWith("ROLE_")); // bất kỳ ROLE_* => nhân viên

        // Ưu tiên quay về trang user định truy cập trước khi login
        SavedRequest saved = requestCache.getRequest(req, res);
        String target = (saved != null) ? saved.getRedirectUrl() : null;

        // Nếu khách hàng cố vào /admin trước đó -> bỏ saved target
        if (!isStaff && target != null && target.contains("/admin")) {
            target = null;
        }

        String ctx = req.getContextPath();
        if (target == null || target.isBlank()) {
            target = isStaff ? (ctx + "/admin/dashboard") : (ctx + "/home");
        }

        // Dọn saved request để tránh vòng lặp
        requestCache.removeRequest(req, res);

        redirectStrategy.sendRedirect(req, res, target);
    }
}
