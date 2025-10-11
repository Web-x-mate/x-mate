package xmate.com.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import xmate.com.service.auth.IUserService;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final IUserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req, HttpServletResponse res, Authentication auth)
            throws java.io.IOException {
        String email = auth.getName(), fullname = email;

        Object p = auth.getPrincipal();
        if (p instanceof OAuth2User oau) {
            var at = oau.getAttributes();
            Object e = at.get("email");
            Object n = at.getOrDefault("name", e);
            email = (e != null ? e.toString() : email);
            fullname = (n != null ? n.toString() : email);
        }

        // upsert user ngay sau khi login OAuth2
        userService.upsertGoogleUser(email, fullname);

        // điều hướng về form cập nhật (nếu muốn tự skip khi có phone thì có thể query và quyết định tại đây)
        res.sendRedirect("/auth/complete");
    }
}
