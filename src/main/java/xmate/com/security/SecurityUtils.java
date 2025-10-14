// src/main/java/xmate/com/security/SecurityUtils.java
package xmate.com.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

public final class SecurityUtils {

    private SecurityUtils() {}

    /** Lấy email từ Authentication, xử lý cả OAuth2 lẫn form/JWT */
    public static String resolveEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object p = auth.getPrincipal();
        if (p instanceof org.springframework.security.oauth2.core.user.OAuth2User oau) {
            Object e = oau.getAttributes().get("email");
            if (e != null) return e.toString();
        }
        // Với form/JWT: getName() thường là email/username
        return auth.getName();
    }
}
