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

    @Override
    @Transactional
    public TokenRes register(RegisterReq req) {
        final String email = norm(req.email());
        if (userRepo.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        Customer u = Customer.builder()
                .email(email)
                .passwordHash(encoder.encode(req.password()))
                .fullname(req.fullname())
                .phone(req.phone())
                .enabled(true)
                .build();

        userRepo.save(u);
        return issueTokens(u);
    }

    @Override
    public TokenRes login(LoginReq req) {
        final String email = norm(req.email());
        Customer u = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Sai email/mật khẩu"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new RuntimeException("Sai email/mật khẩu");
        }
        return issueTokens(u);
    }

    @Override
    public TokenRes refresh(RefreshReq req) {
        RefreshToken token = rtRepo.findByToken(req.refreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ"));
        if (token.getRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token hết hạn");
        }
        return issueTokens(token.getCustomer());
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

    public TokenRes issueTokens(Customer u) {
        String access = jwt.generateAccess(u.getEmail(), Map.of("actor", "customer"));
        RefreshToken rt = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .customer(u)
                .expiresAt(Instant.now().plusMillis(refreshExp))
                .revoked(false)
                .build();
        rtRepo.save(rt);
        return new TokenRes(access, rt.getToken());
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
