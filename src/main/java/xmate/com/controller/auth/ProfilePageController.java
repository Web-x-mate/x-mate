// src/main/java/vn/hieesu/project/web/ProfilePageController.java
package xmate.com.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.server.ResponseStatusException;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.CustomerRepository;

@Controller
@RequiredArgsConstructor
public class ProfilePageController {

    private final CustomerRepository userRepo;

    @GetMapping("/user/profile")
    public String profile(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/auth/login";
        }

        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được email");
        }

        // ⬇️ Không còn orElseThrow()
        var opt = userRepo.findByEmailIgnoreCase(email);

        if (opt.isEmpty()) {
            return "redirect:/auth/complete";
        }

        Customer u = opt.get();

        // Case 2: có user nhưng chưa có sđt -> tiếp tục flow complete
        if (u.getPhone() == null || u.getPhone().isBlank()) {
            return "redirect:/auth/complete";
        }

        // OK -> vào trang profile
        model.addAttribute("user", u);
        model.addAttribute("me", u);
        model.addAttribute("active", "profile"); // để sidebar highlight
        model.addAttribute("mb", null);
        model.addAttribute("wallet", null);
        return "user/profile";
    }


    private String resolveEmail(Authentication auth){
        var p = auth.getPrincipal();
        if (p instanceof OAuth2User oau) {
            var e = oau.getAttributes().get("email");
            if (e != null) return e.toString();
        }
        return auth.getName();
    }
}
