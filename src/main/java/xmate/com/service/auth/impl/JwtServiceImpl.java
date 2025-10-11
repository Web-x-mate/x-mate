package xmate.com.service.auth.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.stereotype.Service;
import xmate.com.security.JwtUtils;
import xmate.com.service.auth.JwtService;

import java.util.Date;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtUtils jwtUtils;

    public JwtServiceImpl(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String extractUsername(String token) {
        Jws<Claims> jws = jwtUtils.parse(token);
        return jws.getBody().getSubject();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            var claims = jwtUtils.parse(token).getBody();
            var exp = claims.getExpiration();
            return exp == null || exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    @Override
    public String generateToken(String username) {
        // Có thể thêm claims nếu muốn (roles, provider, ...); tạm để trống
        return jwtUtils.generateAccess(username, java.util.Map.of());
    }

}
