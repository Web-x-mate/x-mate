// xmate/com/web/WebLogoutController.java
package xmate.com.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure; // chỉ dùng mỗi 'secure' từ properties
    @PostMapping("/auth/logout")
    public String webLogout(HttpServletRequest req, HttpServletResponse res){
        var c = WebUtils.getCookie(req, "REFRESH_TOKEN");
        if (c != null && c.getValue()!=null && !c.getValue().isBlank()) {
            try { auth.logout(c.getValue()); } catch (Exception ignored) {}
        }
        // xoá cookies
        clearAuthCookies(res);
        return "redirect:/";
    }
    private void clearAuthCookies(HttpServletResponse res){
        var access = ResponseCookie.from("ACCESS_TOKEN", "")
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/").maxAge(0).build();
        var refresh = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/").maxAge(0).build();
        res.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, access.toString());
        res.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refresh.toString());
    }

}
