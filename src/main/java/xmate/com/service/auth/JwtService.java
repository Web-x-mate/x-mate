package xmate.com.service.auth;

public interface JwtService {
    String extractUsername(String token);
    boolean isTokenValid(String token);

    String generateToken(String username);
}
