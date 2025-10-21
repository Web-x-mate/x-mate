// src/main/java/xmate/com/web/settings/ProfileController.java
package xmate.com.controller.admin.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import xmate.com.entity.system.User;
import xmate.com.service.system.UserProfileService;

@Controller("settingsProfileController")
@RequestMapping("/settings/profile")
@Validated
public class ProfileController {

    private final UserProfileService service;

    public ProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails me, Model model) {
        User user = service.getProfileOf(me.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("form", new UpdateForm(
                user.getFullName(),
                user.getPhone(),
                user.getEmail()
        ));
        return "settings/profile"; // /templates/settings/profile.html
    }

    public static record UpdateForm(
            @NotBlank @Size(min = 2, max = 120) String fullName,
            @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại VN phải 10 chữ số, bắt đầu bằng 0")
            String phone,
            @Email @Size(max = 120) String email
    ) {}

    @PostMapping
    public String update(@AuthenticationPrincipal UserDetails me,
                         @ModelAttribute("form") @Validated UpdateForm form,
                         Model model) {
        try {
            User updated = service.updateSelf(me.getUsername(),
                    form.fullName(), form.phone(), form.email());
            model.addAttribute("user", updated);
            model.addAttribute("saved", true);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("user", service.getProfileOf(me.getUsername()));
        }
        return "settings/profile";
    }
}
