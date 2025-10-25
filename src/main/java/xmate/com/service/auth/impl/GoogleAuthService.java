// src/main/java/xmate/com/service/auth/impl/GoogleAuthService.java
package xmate.com.service.auth.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xmate.com.dto.auth.TokenRes;
import xmate.com.entity.auth.RefreshToken;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.auth.RefreshTokenRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.security.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final CustomerRepository customers;
    private final JwtUtils jwt;                       // tự ký access token
    private final RefreshTokenRepository rtRepo;      // tự lưu refresh token
    private final PasswordEncoder encoder;            // ✅ THÊM encoder

    @Value("${app.jwt.refresh-exp-ms}")
    private long refreshExp;

    @Value("${app.google.client-id}")
    private String clientId;

    public TokenRes loginWithGoogle(String idTokenStr){
        try {
            var verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), new JacksonFactory())
                    .setAudience(List.of(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) throw new RuntimeException("Invalid Google ID token");

            var payload = idToken.getPayload();
            String email = (String) payload.get("email");
            String name  = (String) payload.getOrDefault("name", email);

            Customer u = customers.findByEmailIgnoreCase(email)
                    .orElseGet(() -> {
                        var c = new Customer();
                        c.setEmail(email);
                        c.setFullname(name);
                        c.setEnabled(true);
                        // ✅ đặt mật khẩu ngẫu nhiên đã băm để không bị NULL
                        c.setPasswordHash(encoder.encode("gsi_" + UUID.randomUUID()));
                        return customers.save(c);
                    });

            // --- phát token như cũ ---
            String access = jwt.generateAccess(u.getEmail(), Map.of("actor", "customer","authm", "google"));
            RefreshToken rt = RefreshToken.builder()
                    .token("g." + UUID.randomUUID())
                    .customer(u)
                    .expiresAt(Instant.now().plusMillis(refreshExp))
                    .revoked(false)
                    .build();
            rtRepo.save(rt);

            return new TokenRes(access, rt.getToken());
        } catch (Exception e){
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }

}