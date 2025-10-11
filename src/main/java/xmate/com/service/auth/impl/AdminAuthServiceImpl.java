package xmate.com.service.auth.impl;

// xmate.com.service.auth.impl.AdminAuthServiceImpl

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import xmate.com.dto.auth.LoginReq;
import xmate.com.dto.auth.TokenRes;
import xmate.com.entity.system.Role;
import xmate.com.repo.system.UserRepository;
import xmate.com.security.JwtUtils;
import xmate.com.service.auth.IAdminAuthService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements IAdminAuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;

    @Override
    public TokenRes login(LoginReq req) {
        String username = norm(req.email()); // tái dùng field email làm username input
        var u = userRepo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RuntimeException("Sai tài khoản/mật khẩu"));
        if (!Boolean.TRUE.equals(u.getActive()) || !encoder.matches(req.password(), u.getPassword())) {
            throw new RuntimeException("Sai tài khoản/mật khẩu");
        }

        // lấy roles từ DB → claim "roles"
        var roles = u.getRoles() == null ? List.<String>of()
                : u.getRoles().stream()
                .map(Role::getName)          // ADMIN / MANAGER / ...
                .map(s -> s == null ? "" : s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .toList();

        String access = jwt.generateAccess(u.getUsername(), Map.of(
                "actor", "staff",
                "roles", roles
        ));
        // đơn giản: không cấp refresh cho admin (an toàn hơn); nếu muốn có, tạo bảng admin_refresh_tokens riêng
        return new TokenRes(access, null);
    }

    private static String norm(String s){ return s==null?null:s.trim().toLowerCase(); }
}

