package xmate.com.api;

import lombok.RequiredArgsConstructor;
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
    public TokenRes login(@RequestBody LoginReq req) {
        return adminAuth.login(req);
    }
}