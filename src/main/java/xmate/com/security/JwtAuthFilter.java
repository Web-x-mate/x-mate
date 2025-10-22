package xmate.com.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import xmate.com.service.auth.JwtService;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String p = request.getServletPath();
        // Static, trang public
        if (p.startsWith("/css/") || p.startsWith("/js/") || p.startsWith("/images/")
                || p.startsWith("/client/")
                || p.startsWith("/webjars/") || p.startsWith("/favicon")
                || p.equals("/") || p.equals("/auth/login") || p.startsWith("/auth/register")
                || p.startsWith("/oauth2/")) {
            return true;
        }
        // ⬇⬇⬇ BỎ LỌC CHO TOÀN BỘ API AUTH
        if (p.startsWith("/api/auth/")) {
            return true;
        }
        // Bỏ lọc cho preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return false;
    }



    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        // Nếu đã có Authentication thì bỏ qua
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        // 1) Lấy token: ưu tiên header, fallback cookie
        String token = null;

        String authz = request.getHeader("Authorization");
        if (authz != null && authz.startsWith("Bearer ")) {
            token = authz.substring(7);
        } else if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("ACCESS_TOKEN".equals(c.getName())) {
                    token = c.getValue();
                    break;
                }
            }
        }

        // Không có token -> cho qua
        if (token == null || token.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // 2) Validate & set Authentication
        try {
            if (jwtService.isTokenValid(token)) {
                String username = jwtService.extractUsername(token);
                if (username != null) {
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                clearAccessTokenCookie(response);
            }
        } catch (JwtException | IllegalArgumentException ex) {
            clearAccessTokenCookie(response);
        }

        chain.doFilter(request, response);
    }

    private void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie expired = new Cookie("ACCESS_TOKEN", "");
        expired.setHttpOnly(true);
        expired.setPath("/");
        expired.setMaxAge(0);
        response.addCookie(expired);
    }

}
