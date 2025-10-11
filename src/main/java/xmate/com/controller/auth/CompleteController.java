package xmate.com.controller.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xmate.com.entity.customer.Customer;        // <-- import entity của bạn
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.auth.IUserService;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CompleteController {
    private final CustomerRepository userRepo;
    private final IUserService userService;

    // SAU LOGIN: vào đây trước
    @GetMapping("/auth/complete")
    public String showComplete(Authentication auth, Model model) {
        if (auth == null) return "redirect:/auth/login";
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return "redirect:/auth/login";
        String fullName = displayName(auth, null, email);

        // tạo nếu chưa có (qua service -> có password_hash)
        Customer u = getOrCreateUser(email, fullName);

        // đã có phone?
        if (hasPhone(u)) {
            // đủ hồ sơ?
            if (isProfileComplete(u)) {
                return "redirect:/user/profile";
            }
            return "redirect:/auth/complete/intro";
        }

        // chưa có phone -> ở lại trang nhập SĐT
        model.addAttribute("user", java.util.Map.of(
                "fullname", displayName(auth, u.getFullname(), email),
                "email", email
        ));
        return "auth/complete";
    }

    // Lưu sđt từ form ở trang complete
    @PostMapping("/auth/complete/phone")
    public String savePhone(@RequestParam String phone, Authentication auth) {
        if (auth == null) return "redirect:/auth/login";
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return "redirect:/auth/login";
        String fullName = displayName(auth, null, email);

        Customer u = getOrCreateUser(email, fullName);

        String normalized = normalizeToE164VN(phone);
        if (normalized == null) return "redirect:/auth/complete?err=invalid";

        u.setPhone(normalized);
        try {
            userRepo.save(u); // phone unique
            return "redirect:/auth/complete/intro";
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            return "redirect:/auth/complete?err=taken";
        }
    }
    @PostMapping("/auth/complete/intro")
    public String saveIntro(Authentication auth,
                            @RequestParam(required = false) Integer height,
                            @RequestParam(required = false) Integer weight,
                            @RequestParam(required = false) String gender,
                            @RequestParam(required = false) String dob // yyyy-MM-dd hoặc rỗng
    ) {
        if (auth == null) return "redirect:/auth/login";
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return "redirect:/auth/login";

        var u = getOrCreateUser(email, displayName(auth, null, email));

        if (height != null) u.setHeightCm(height);
        if (weight != null) u.setWeightKg(weight);
        if (gender != null && !gender.isBlank()) u.setGender(gender);

        if (dob != null && !dob.isBlank()) {
            // parse yyyy-MM-dd -> LocalDate
            u.setDob(java.time.LocalDate.parse(dob));
        }

        userRepo.save(u);
        return "redirect:/user/profile";
    }
    @GetMapping("/auth/complete/intro/skip")
    public String skipIntro(Authentication auth) {
        if (auth == null) return "redirect:/auth/login";
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return "redirect:/auth/login";

        // đã có (hoặc tạo) user
        String fullName = displayName(auth, null, email);
        Customer u = getOrCreateUser(email, fullName);

        // Nếu chưa có SĐT -> quay lại bước nhập SĐT
        if (!hasPhone(u)) return "redirect:/auth/complete";

        // Có SĐT rồi:
        // - nếu đã đủ hồ sơ -> vào thẳng profile
        // - nếu chưa đủ hồ sơ -> vẫn cho vào profile (vì bạn muốn “bỏ qua”)
        return "redirect:/user/profile";
    }



    // Trang intro: chỉ cho vào khi đã có phone
    @GetMapping("/auth/complete/intro")
    public String showIntro(Authentication auth, Model model) {
        if (auth == null) return "redirect:/auth/login";
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return "redirect:/auth/login";
        String fullName = displayName(auth, null, email);

        Customer u = getOrCreateUser(email, fullName);

        if (!hasPhone(u)) return "redirect:/auth/complete"; // chưa có phone -> quay lại nhập
        if (isProfileComplete(u)) return "redirect:/user/profile"; // đủ rồi -> thẳng profile

        model.addAttribute("name", displayName(auth, u.getFullname(), email));
        model.addAttribute("hDefault", u.getHeightCm() != null ? u.getHeightCm() : 174);
        model.addAttribute("wDefault", u.getWeightKg() != null ? u.getWeightKg() : 62);
        return "auth/complete-intro";
    }

    // --- helpers ---
    private Customer getOrCreateUser(String email, String fullName) {
        return userRepo.findByEmailIgnoreCase(email).orElseGet(() -> {
            userService.upsertGoogleUser(email, fullName); // đảm bảo có password_hash
            return userRepo.findByEmailIgnoreCase(email).orElseThrow();
        });
    }

    private boolean hasPhone(Customer u) {
        return u.getPhone() != null && !u.getPhone().isBlank();
    }

    // Tự định nghĩa “đủ hồ sơ” theo nhu cầu bạn
    private boolean isProfileComplete(Customer u) {
        // ví dụ: cần đủ phone + chiều cao + cân nặng (có thể bổ sung DOB, address…)
        return hasPhone(u) && u.getHeightCm() != null && u.getWeightKg() != null;
    }

    private String resolveEmail(Authentication auth) {
        Object p = auth.getPrincipal();
        if (p instanceof org.springframework.security.oauth2.core.user.OAuth2User oau) {
            Object e = oau.getAttributes().get("email");
            if (e == null) e = oau.getAttributes().get("preferred_username"); // fallback
            return e != null ? e.toString() : null;
        }
        // form-login: username của bạn PHẢI là email để logic này đúng
        return auth.getName();
    }

    private String displayName(Authentication auth, String dbFullname, String fallbackEmail) {
        if (dbFullname != null && !dbFullname.isBlank()) return dbFullname;
        Object p = auth.getPrincipal();
        if (p instanceof org.springframework.security.oauth2.core.user.OAuth2User oau) {
            Object n = oau.getAttributes().get("name");
            if (n == null) n = oau.getAttributes().get("given_name");
            if (n != null && !n.toString().isBlank()) return n.toString();
        }
        return fallbackEmail;
    }

    private String normalizeToE164VN(String raw){
        if (raw == null) return null;
        String s = raw.replaceAll("[^0-9+]", "");
        if (s.startsWith("+84")) return s.substring(3).matches("\\d{9,10}") ? s : null;
        if (s.startsWith("0") && s.length()>=10 && s.length()<=11) return "+84"+s.substring(1);
        if (s.matches("\\d{9,10}")) return "+84"+s;
        return null;
    }
}
