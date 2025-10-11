package xmate.com.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

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
