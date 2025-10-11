package xmate.com.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@Controller
public class PageController {
    // === PUBLIC PAGES ===
    @GetMapping({"/", "/auth/login"})
    public String login() { return "auth/login"; }

    @GetMapping("/auth/register")
    public String register() { return "auth/register"; }

    @GetMapping("/auth/forgot")
    public String forgot() { return "auth/forgot"; }
    // === AUTH PAGES ===


    @GetMapping("/user/addresses")
    public String addresses() { return "user/addresses"; }
}
