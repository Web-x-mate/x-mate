package xmate.com.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import xmate.com.dto.auth.*;
import xmate.com.entity.customer.Address;
import xmate.com.security.RecaptchaService;
import xmate.com.service.auth.IAuthService;
import xmate.com.service.auth.IUserService;
import xmate.com.dto.auth.FacebookLoginReq;
import lombok.RequiredArgsConstructor;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {
    private final IAuthService auth;
    private final IUserService users;
    private final RecaptchaService recaptcha;
//    public AuthController(IAuthService auth, IUserService users) {
//        this.auth = auth; this.users = users;
//    }

    // ==== AUTH (local) ====
    @PostMapping("/auth/register")
    public TokenRes register(@RequestBody @Valid RegisterReq req,
                             HttpServletRequest http,
                             HttpServletResponse res){
        if (req.recaptchaToken() == null || !recaptcha.verify(req.recaptchaToken(), http.getRemoteAddr())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha không hợp lệ.");
        }
        TokenRes t = auth.register(req);
        setAuthCookies(res, t);
        return t;
    }

    @PostMapping("/auth/login")
    public TokenRes login(@RequestBody @Valid LoginReq req, HttpServletResponse res){
        TokenRes t = auth.login(req);
        setAuthCookies(res, t);
        return t;
    }

    // ==== AUTH (Google ID Token) ====
    @PostMapping("/auth/google")
    public TokenRes loginWithGoogle(@RequestBody @Valid GoogleLoginReq req, HttpServletResponse res){
        TokenRes t = auth.loginWithGoogle(req.idToken());
        setAuthCookies(res, t);
        return t;
    }

    // ==== REFRESH: set lại cookie ACCESS_TOKEN ====
    @PostMapping("/auth/refresh")
    public TokenRes refresh(@RequestBody @Valid RefreshReq req, HttpServletResponse res){
        TokenRes t = auth.refresh(req);

        ResponseCookie access = ResponseCookie.from("ACCESS_TOKEN", t.accessToken())
                .httpOnly(true).secure(cookieSecure)
                .sameSite("Lax").path("/").build();
        res.addHeader("Set-Cookie", access.toString());

        // Nếu refresh() của bạn "rotate" refresh token → set lại REFRESH_TOKEN luôn:
        ResponseCookie refresh = ResponseCookie.from("REFRESH_TOKEN", t.refreshToken())
                .httpOnly(true).secure(cookieSecure)
                .sameSite("Lax").path("/").build();
        res.addHeader("Set-Cookie", refresh.toString());

        if (t.userToken() != null && !t.userToken().isBlank()) {
            ResponseCookie userToken = ResponseCookie.from("token_user", t.userToken())
                    .httpOnly(false).secure(cookieSecure)
                    .sameSite("Lax").path("/").build();
            res.addHeader("Set-Cookie", userToken.toString());
        }

        return t;
    }

    // ==== LOGOUT: revoke refresh + xoá cookies ====
    @PostMapping("/auth/logout")
    public void logout(@RequestBody @Valid RefreshReq req, HttpServletResponse res){
        auth.logout(req.refreshToken());
        clearAuthCookies(res);
    }

    // ==== ME (JWT Bearer) ====
    @GetMapping("/me")
    public MeRes me(Authentication authn){
        return users.getMe(authn.getName());
    }

    @PutMapping("/me")
    public MeRes updateMe(Authentication authn, @RequestBody UpdateMeReq req){
        return users.updateMe(authn.getName(), req);
    }

    @GetMapping("/me/addresses")
    public List<Address> myAddresses(Authentication authn){
        return users.getMyAddresses(authn.getName());
    }

    @PostMapping("/me/addresses")
    public Address addAddress(Authentication authn, @RequestBody AddressReq req){
        return users.addAddress(authn.getName(), req);
    }
    @PostMapping("/auth/facebook")
    public TokenRes loginWithFacebook(@RequestBody @Valid FacebookLoginReq req,
                                      HttpServletResponse res) {
        TokenRes t = auth.loginWithFacebook(req.accessToken()); // gọi service
        setAuthCookies(res, t);
        return t;
    }
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;
    // ===== cookie helpers =====
    private void setAuthCookies(HttpServletResponse res, TokenRes t){
        ResponseCookie access = ResponseCookie.from("ACCESS_TOKEN", t.accessToken())
                .httpOnly(true).secure(cookieSecure) // prod: true (HTTPS)
                .path("/").sameSite("Lax").build();
        ResponseCookie refresh = ResponseCookie.from("REFRESH_TOKEN", t.refreshToken())
                .httpOnly(true).secure(cookieSecure) // prod: true (HTTPS)
                .path("/").sameSite("Lax").build();
        res.addHeader("Set-Cookie", access.toString());
        res.addHeader("Set-Cookie", refresh.toString());
        if (t.userToken() != null && !t.userToken().isBlank()) {
            ResponseCookie userToken = ResponseCookie.from("token_user", t.userToken())
                    .httpOnly(false).secure(cookieSecure)
                    .path("/").sameSite("Lax").build();
            res.addHeader("Set-Cookie", userToken.toString());
        }
    }

    private void clearAuthCookies(HttpServletResponse res){
        res.addHeader("Set-Cookie", "ACCESS_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        res.addHeader("Set-Cookie", "REFRESH_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        res.addHeader("Set-Cookie", "token_user=; Path=/; Max-Age=0; SameSite=Lax");
    }
}

