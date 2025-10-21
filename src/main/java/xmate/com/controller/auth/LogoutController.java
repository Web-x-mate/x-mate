// xmate/com/web/WebLogoutController.java
package xmate.com.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.util.WebUtils;
import xmate.com.dto.auth.RefreshReq;
import xmate.com.service.auth.IAuthService;

@Controller
public class LogoutController {

    private final IAuthService auth;
    public LogoutController(IAuthService auth){ this.auth = auth; }

    @PostMapping("/auth/logout")
    public String webLogout(HttpServletRequest req, HttpServletResponse res){
        var c = WebUtils.getCookie(req, "REFRESH_TOKEN");
        if (c != null && c.getValue()!=null && !c.getValue().isBlank()) {
            try { auth.logout(c.getValue()); } catch (Exception ignored) {}
        }
        // xoá cookies
        res.addHeader("Set-Cookie", "ACCESS_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        res.addHeader("Set-Cookie", "REFRESH_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        return "redirect:/";
    }
}
