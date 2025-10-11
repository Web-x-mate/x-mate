package xmate.com.service.auth;

import xmate.com.dto.auth.LoginReq;
import xmate.com.dto.auth.TokenRes;

// xmate.com.service.auth.IAdminAuthService
public interface IAdminAuthService {
    TokenRes login(LoginReq req);
}
