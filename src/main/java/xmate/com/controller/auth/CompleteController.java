package xmate.com.controller.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.CustomerRepository;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CompleteController {

    private final CustomerRepository userRepo;

    // SAU LOGIN: vào đây trước
    @GetMapping("/auth/complete")
    public String showComplete(Authentication auth, Model model) {
        String email = resolveEmail(auth);
        if (email == null) return "redirect:/auth/login";

        Customer u = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return "redirect:/auth/login"; // chưa có user -> chưa login social qua API

        // đã có phone?
        if (hasPhone(u)) {
            if (isProfileComplete(u)) return "redirect:/user/profile";
            return "redirect:/auth/complete/intro";
        }

        model.addAttribute("user", java.util.Map.of(
                "fullname", (u.getFullname() != null && !u.getFullname().isBlank()) ? u.getFullname() : email,
                "email", email
        ));
        return "auth/complete";
    }

    @PostMapping("/auth/complete/phone")
    public String savePhone(@RequestParam String phone, Authentication auth) {
        String email = resolveEmail(auth);
        if (email == null) return "redirect:/auth/login";
        Customer u = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return "redirect:/auth/login";

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
                            @RequestParam(required = false) String dob) {
        String email = resolveEmail(auth);
        if (email == null) return "redirect:/auth/login";
        Customer u = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return "redirect:/auth/login";

        if (height != null) u.setHeightCm(height);
        if (weight != null) u.setWeightKg(weight);
        if (gender != null && !gender.isBlank()) u.setGender(gender);
        if (dob != null && !dob.isBlank()) u.setDob(java.time.LocalDate.parse(dob));

        userRepo.save(u);
        return "redirect:/user/profile";
    }

    @GetMapping("/auth/complete/intro")
    public String showIntro(Authentication auth, Model model) {
        String email = resolveEmail(auth);
        if (email == null) return "redirect:/auth/login";
        Customer u = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return "redirect:/auth/login";

        if (!hasPhone(u)) return "redirect:/auth/complete";
        if (isProfileComplete(u)) return "redirect:/user/profile";

        model.addAttribute("name", (u.getFullname() != null && !u.getFullname().isBlank()) ? u.getFullname() : email);
        model.addAttribute("hDefault", u.getHeightCm() != null ? u.getHeightCm() : 174);
        model.addAttribute("wDefault", u.getWeightKg() != null ? u.getWeightKg() : 62);
        return "auth/complete-intro";
    }

    @GetMapping("/auth/complete/intro/skip")
    public String skipIntro(Authentication auth) {
        String email = resolveEmail(auth);
        if (email == null) return "redirect:/auth/login";
        Customer u = userRepo.findByEmailIgnoreCase(email).orElse(null);
        if (u == null) return "redirect:/auth/login";

        if (!hasPhone(u)) return "redirect:/auth/complete";
        return "redirect:/user/profile";
    }

    // --- helpers ---
    private String resolveEmail(Authentication auth) {
        return (auth != null) ? auth.getName() : null; // JwtAuthFilter set username = email
    }
    private boolean hasPhone(Customer u) {
        return u.getPhone() != null && !u.getPhone().isBlank();
    }
    private boolean isProfileComplete(Customer u) {
        return hasPhone(u) && u.getHeightCm() != null && u.getWeightKg() != null;
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
