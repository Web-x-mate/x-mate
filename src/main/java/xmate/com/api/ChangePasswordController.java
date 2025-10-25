// src/main/java/xmate/com/api/ChangePasswordController.java
package xmate.com.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import xmate.com.repo.customer.CustomerRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ChangePasswordController {

    private final CustomerRepository userRepo;
    private final PasswordEncoder encoder;

    public record ChangePasswordReq(String oldP, @NotBlank String newP) {}

    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<?> change(Authentication auth, @RequestBody @Valid ChangePasswordReq req) {
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).body("unauthorized");

        var u = userRepo.findByEmailIgnoreCase(auth.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(404).body("user not found");

        String authm = resolveAuthm(auth); // local | google | facebook

        // local -> bắt buộc oldP
        if ("local".equalsIgnoreCase(authm)) {
            if (req.oldP() == null || req.oldP().isBlank()) return ResponseEntity.badRequest().body("oldP required");
            if (!encoder.matches(req.oldP(), u.getPasswordHash())) {
                return ResponseEntity.badRequest().body("old password incorrect");
            }
        }

        // kiểm tra độ mạnh
        if (!strong(req.newP())) return ResponseEntity.badRequest().body("weak password");

        // cập nhật
        u.setPasswordHash(encoder.encode(req.newP()));
        userRepo.save(u);

        // (tuỳ chọn) revoke refresh tokens cũ tại đây nếu muốn buộc đăng nhập lại

        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private String resolveAuthm(Authentication auth){
        Object d = auth.getDetails();
        if (d instanceof Map<?,?> m) {
            Object v = ((Map<String,Object>) m).get("authm");
            if (v != null) return v.toString();
        }
        return "local";
    }

    private boolean strong(String p){
        return p != null && p.length()>=8
                && p.matches(".*[A-Z].*")
                && p.matches(".*[a-z].*")
                && p.matches(".*\\d.*")
                && p.matches(".*[^A-Za-z0-9].*");
    }
}
