package xmate.com.service.auth.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import xmate.com.dto.auth.TokenRes;
import xmate.com.entity.auth.RefreshToken;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.auth.RefreshTokenRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.security.JwtUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class FacebookAuthService {

    private final RestClient http;
    private final CustomerRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtils jwt;
    private final RefreshTokenRepository rtRepo;

    @Value("${app.jwt.refresh-exp-ms}")
    private long refreshExp;

    @Value("${app.facebook.app-id}")
    private String appId;

    @Value("${app.facebook.app-secret}")
    private String appSecret;

    // dùng RestClient.Builder có sẵn của Spring Boot, không cần bean riêng
    public FacebookAuthService(RestClient.Builder builder,
                               CustomerRepository userRepo,
                               PasswordEncoder encoder,
                               JwtUtils jwt,
                               RefreshTokenRepository rtRepo) {
        this.http = builder.build();
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwt = jwt;
        this.rtRepo = rtRepo;
    }

    public TokenRes loginWithFacebook(String userAccessToken) {
        DebugToken dt = debugToken(userAccessToken);
        if (dt == null || !dt.data.is_valid) {
            throw new RuntimeException("Facebook token không hợp lệ");
        }
        if (!appId.equals(dt.data.app_id)) {
            throw new RuntimeException("Facebook token không thuộc app này");
        }

        Me me = fetchMe(userAccessToken);
        if (me == null || me.id == null) {
            throw new RuntimeException("Không lấy được thông tin người dùng Facebook");
        }

        String email = (me.email != null && !me.email.isBlank())
                ? normalize(me.email)
                : "fb-" + me.id + "@facebook.local";

        Customer u = userRepo.findByEmailIgnoreCase(email).orElseGet(() -> {
            String pseudo = "fb:" + me.id + ":" + System.currentTimeMillis();
            Customer c = Customer.builder()
                    .email(email)
                    .fullname(me.name)
                    .passwordHash(encoder.encode(pseudo))
                    .enabled(true)
                    .build();
            return userRepo.save(c);
        });

        return issueTokens(u);
    }

    private TokenRes issueTokens(Customer u) {
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

    private DebugToken debugToken(String userToken) {
        String appToken = appId + "|" + appSecret;
        return http.get()
                .uri("https://graph.facebook.com/debug_token?input_token={ut}&access_token={at}",
                        userToken, appToken)
                .retrieve()
                .body(DebugToken.class);
    }

    private Me fetchMe(String userToken) {
        return http.get()
                .uri("https://graph.facebook.com/me?fields=id,name,email&access_token={ut}", userToken)
                .retrieve()
                .body(Me.class);
    }

    private static String normalize(String s){ return s == null ? null : s.trim().toLowerCase(); }

    public record DebugToken(Data data) { public record Data(boolean is_valid, String app_id, String user_id, Long expires_at) {} }
    public record Me(String id, String name, String email) {}
}
