package xmate.com.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.auth.ProfileCompleteReq;
import xmate.com.repo.customer.CustomerRepository;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {
    private final CustomerRepository userRepo;
    @PostMapping(value="/profile/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> completeForm(Authentication auth, @ModelAttribute ProfileCompleteReq req) {
        return doComplete(auth, req);
    }

    @PostMapping(value="/profile/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> completeJson(Authentication auth, @RequestBody ProfileCompleteReq req) {
        return doComplete(auth, req);
    }

    public ResponseEntity<?> doComplete(Authentication auth, ProfileCompleteReq req) {
        // 1) Auth
        if (auth == null) return ResponseEntity.status(401).body("unauthorized");
        String email = resolveEmail(auth); // đừng dùng auth.getName() cho OAuth2
        var u = userRepo.findByEmailIgnoreCase(email)
                .orElse(null);
        if (u == null) return ResponseEntity.status(404).body("user not found");

        // 2) DOB (an toàn nếu client chưa chọn đủ ngày/tháng/năm)
        if (req.dob() != null && !req.dob().isBlank()) {
            try {
                u.setDob(java.time.LocalDate.parse(req.dob()));  // yyyy-MM-dd
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("dob invalid");
            }
        }

        // 3) Gender/Height/Weight (tự nguyện)
        if (req.gender() != null) u.setGender(req.gender());
        if (req.heightCm() != null) u.setHeightCm(req.heightCm());
        if (req.weightKg() != null) u.setWeightKg(req.weightKg());

        // 4) Phone (nếu user chưa có thì bắt buộc nhập; nếu có thì cho phép đổi)
        String incoming = req.phone();
        if (isBlank(u.getPhone())) {
            if (isBlank(incoming)) return ResponseEntity.badRequest().body("phone required");
            String norm = normalizeToE164VN(incoming);
            if (norm == null) return ResponseEntity.badRequest().body("phone invalid");
            // CHECK trùng (không cho số đã có người dùng khác dùng)
            if (userRepo.existsByPhone(norm)) {
                return ResponseEntity.status(409).body("phone already in use");
            }
            u.setPhone(norm);
        } else if (!isBlank(incoming)) {
            String norm = normalizeToE164VN(incoming);
            if (norm == null) return ResponseEntity.badRequest().body("phone invalid");
            if (!norm.equals(u.getPhone())) {
                if (userRepo.existsByPhoneAndIdNot(norm, u.getId())) {
                    return ResponseEntity.status(409).body("phone already in use");
                }
                u.setPhone(norm);
            }
        }

        try {
            userRepo.save(u);             // unique(phone) => có thể ném DIve
            return ResponseEntity.ok().build();
        } catch (org.springframework.dao.DataIntegrityViolationException dup) {
            return ResponseEntity.status(409).body("phone already in use");
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body("server error");
        }
    }

    // ---- helpers ----
    private String resolveEmail(Authentication auth) {
        Object p = auth.getPrincipal();
        if (p instanceof org.springframework.security.oauth2.core.user.OAuth2User oau) {
            Object e = oau.getAttributes().get("email");
            if (e != null) return e.toString();
        }
        return auth.getName();
    }
    private boolean isBlank(String s){ return s==null || s.isBlank(); }

    /** Chuẩn hoá số VN => E.164 (+84xxxxxxxxx) */
    private String normalizeToE164VN(String raw){
        if (raw == null) return null;
        String s = raw.replaceAll("[^0-9+]", "");
        if (s.startsWith("+84")) return s.substring(3).matches("\\d{9,10}") ? s : null;
        if (s.startsWith("0") && s.length()>=10 && s.length()<=11) return "+84"+s.substring(1);
        if (s.matches("\\d{9,10}")) return "+84"+s;
        return null;
    }
    @GetMapping("/profile/phone-exists")
    public Map<String, Object> phoneExists(Authentication auth, @RequestParam String phone) {
        // 1) Chuẩn hoá về E.164 (+84xxxxxxxxx). Trả "invalid" nếu không phải số VN hợp lệ
        String norm = normalizeToE164VN(phone);
        if (norm == null) {
            return Map.of("valid", false, "exists", false, "reason", "invalid");
        }

        // 2) Check trùng – loại trừ chính user hiện tại nếu có
        var me = userRepo.findByEmailIgnoreCase(resolveEmail(auth)).orElse(null);
        boolean exists = (me == null)
                ? userRepo.existsByPhone(norm)
                : userRepo.existsByPhoneAndIdNot(norm, me.getId());

        // 3) valid = !exists (hợp lệ để sử dụng nếu chưa bị trùng)
        return Map.of("valid", !exists, "exists", exists, "normalized", norm);
    }


}





