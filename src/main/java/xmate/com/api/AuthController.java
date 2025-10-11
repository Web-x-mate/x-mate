package xmate.com.api;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.auth.*;
import xmate.com.entity.customer.Address;
import xmate.com.service.auth.IAuthService;
import xmate.com.service.auth.IUserService;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final IAuthService auth;
    private final IUserService users;

    public AuthController(IAuthService auth, IUserService users) {
        this.auth = auth; this.users = users;
    }

    // ---- AUTH
    @PostMapping("/auth/register")
    public TokenRes register(@RequestBody @Valid RegisterReq req){ return auth.register(req); }

    @PostMapping("/auth/login")
    public TokenRes login(@RequestBody @Valid LoginReq req){ return auth.login(req); }

    @PostMapping("/auth/refresh")
    public TokenRes refresh(@RequestBody @Valid RefreshReq req){ return auth.refresh(req); }

    @PostMapping("/auth/logout")
    public void logout(@RequestBody RefreshReq req){ auth.logout(req.refreshToken()); }

    // ---- ME (JWT Bearer)
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
}
