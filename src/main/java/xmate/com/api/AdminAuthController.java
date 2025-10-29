package xmate.com.api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xmate.com.dto.auth.LoginReq;
import xmate.com.dto.auth.TokenRes;
import xmate.com.service.auth.IAdminAuthService;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {
    private final IAdminAuthService adminAuth;

    @PostMapping("/login")
    public TokenRes login(@RequestBody LoginReq req, HttpServletResponse res) {
        TokenRes t = adminAuth.login(req);
        ResponseCookie access = ResponseCookie.from("ACCESS_TOKEN", t.accessToken())
                .httpOnly(true)   // prod: true + HTTPS
                .secure(false)    // prod: true
                .sameSite("Lax")
                .path("/")
                .build();
        res.addHeader("Set-Cookie", access.toString());
        return t;
    }
}