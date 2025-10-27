package xmate.com.service.auth.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.dto.auth.LoginReq;
import xmate.com.dto.auth.RefreshReq;
import xmate.com.dto.auth.RegisterReq;
import xmate.com.dto.auth.TokenRes;
import xmate.com.entity.auth.RefreshToken;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.auth.RefreshTokenRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.security.JwtUtils;
import xmate.com.service.auth.IAuthService;

import java.time.Instant;
import java.util.*;

@Service
public class AuthServiceImpl implements IAuthService {

    private final CustomerRepository userRepo;
    private final RefreshTokenRepository rtRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;
    private final long refreshExp;
    private final FacebookAuthService facebookAuthService;
    private final GoogleAuthService googleAuthService;
    public AuthServiceImpl(CustomerRepository userRepo,
                           RefreshTokenRepository rtRepo,
                           PasswordEncoder encoder,
                           JwtUtils jwt,
                           @Value("${app.jwt.refresh-exp-ms}") long refreshExp,
                           GoogleAuthService googleAuthService,
                           FacebookAuthService facebookAuthService) {
        this.userRepo = userRepo;
        this.rtRepo = rtRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshExp = refreshExp;
        this.googleAuthService = googleAuthService;
        this.facebookAuthService = facebookAuthService;
    }
//    Chuan hoa sdt -> vn
    private static String normEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    // Chuẩn hoá số VN về E.164 (+84…)
    private static String normPhoneVN(String raw) {
        if (raw == null) return null;
        // 1) bỏ mọi ký tự không phải số (loại bỏ khoảng trắng, '-', '+', ...)
        String d = raw.replaceAll("\\D+", "");
        if (d.isEmpty()) return null;

        // 2) cắt các tiền tố phổ biến
        if (d.startsWith("84")) {
            d = d.substring(2); // "84xxxxxxxxx" -> "xxxxxxxxx"
        }
        if (d.startsWith("0")) {
            d = d.substring(1); // "0xxxxxxxxx"  -> "xxxxxxxxx"
        }

        // 3) ghép lại +84
        return "+84" + d;
    }

    @Override
    @Transactional
    public TokenRes register(RegisterReq req) {
        final String email = norm(req.email());
        final String phoneE164 = normPhoneVN(req.phone());
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }
        if (phoneE164 != null && userRepo.existsByPhone(phoneE164)) {
            throw new RuntimeException("Số điện thoại đã tồn tại");
        }
        Customer u = Customer.builder()
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .fullname(req.fullname())
                .phone(phoneE164)
                .enabled(true)
                .build();

        userRepo.save(u);
        return issueTokens(u, "local");
    }

    @Override
    public TokenRes login(LoginReq req) {
        final String email = norm(req.email());
        Customer u = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Sai email/mật khẩu"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new RuntimeException("Sai email/mật khẩu");
        }
        return issueTokens(u, "local");
    }

    @Override
    public TokenRes refresh(RefreshReq req) {
        RefreshToken token = rtRepo.findByToken(req.refreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));
        if (token.getRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token hết hạn");
        }
        String authm = guessAuthmFromRefresh(token.getToken());
        return issueTokens(token.getCustomer(), authm);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        rtRepo.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            rtRepo.save(rt);
        });
    }

    // ===== helpers =====
    private String guessAuthmFromRefresh(String rt){
        if (rt == null) return "local";
        String s = rt.toLowerCase(Locale.ROOT);
        if (s.startsWith("g.")) return "google";
        if (s.startsWith("f.")) return "facebook";
        if (s.startsWith("l.")) return "local";
        return "local"; // token cũ chưa có prefix -> mặc định local
    }
    private String ensureUserToken(Customer u) {
        if (u.getTokenUser() == null || u.getTokenUser().isBlank()) {
            u.setTokenUser("usr_" + UUID.randomUUID().toString().replace("-", ""));
            userRepo.save(u);
        }
        return u.getTokenUser();
    }
    public TokenRes issueTokens(Customer u) {
        return issueTokens(u, "local");
    }
    public TokenRes issueTokens(Customer u, String authm) {
        String am = (authm == null || authm.isBlank()) ? "local" : authm.toLowerCase(Locale.ROOT);
        String userToken = ensureUserToken(u);
        String access = jwt.generateAccess(u.getEmail(), Map.of("actor", "customer", "authm", am));
        String prefix = switch (am) {
            case "google" -> "g.";
            case "facebook" -> "f.";
            default -> "l.";
        };

        RefreshToken rt = RefreshToken.builder()
                .token(prefix + UUID.randomUUID())
                .customer(u)
                .expiresAt(Instant.now().plusMillis(refreshExp))
                .revoked(false)
                .build();
        rtRepo.save(rt);
        return new TokenRes(access, rt.getToken(), userToken);
    }

    @Override
    public TokenRes loginWithGoogle(String idToken) {
        return googleAuthService.loginWithGoogle(idToken);
    }

    @Override
    public TokenRes loginWithFacebook(String userAccessToken) {
        return facebookAuthService.loginWithFacebook(userAccessToken);
    }



    private static String norm(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}



