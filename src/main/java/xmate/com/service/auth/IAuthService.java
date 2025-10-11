package xmate.com.service.auth;

import xmate.com.dto.auth.LoginReq;
import xmate.com.dto.auth.RefreshReq;
import xmate.com.dto.auth.RegisterReq;
import xmate.com.dto.auth.TokenRes;

public interface IAuthService {
    TokenRes register(RegisterReq req);
    TokenRes login(LoginReq req);
    TokenRes refresh(RefreshReq req);
    void logout(String refreshToken);
}
