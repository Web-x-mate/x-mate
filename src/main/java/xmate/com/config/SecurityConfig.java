// src/main/java/xmate/com/config/SecurityConfig.java
package xmate.com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import xmate.com.security.JwtAuthFilter;
import xmate.com.security.RoleRedirectSuccessHandler;
import xmate.com.security.oauth.CustomOAuth2UserService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final CustomOAuth2UserService oAuth2UserService;

    public SecurityConfig(JwtAuthFilter jwtFilter, CustomOAuth2UserService oAuth2UserService) {
        this.jwtFilter = jwtFilter;
        this.oAuth2UserService = oAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           RoleRedirectSuccessHandler roleRedirect,
                                           AuthenticationProvider daoAuthProvider) throws Exception {

        http
                // API dùng JWT: bỏ CSRF cho /api/**
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))

                // Dùng session khi cần cho form-login / oauth2
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Phân quyền
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/error",
                                "/auth/**", "/api/auth/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().permitAll()
                )

                // Provider xác thực chuẩn
                .authenticationProvider(daoAuthProvider)

                // Form login
                .formLogin(form -> form
                        .loginPage("/auth/login")           // GET hiển thị form
                        .loginProcessingUrl("/auth/login")  // POST submit form
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(roleRedirect)
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )

                // OAuth2 login
                .oauth2Login(o -> o
                        .loginPage("/auth/login")
                        .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                        .successHandler(roleRedirect)
                        .failureHandler((rq, rs, ex) -> { ex.printStackTrace(); rs.sendRedirect("/auth/login?oauth2Error"); })
                )

                // Logout -> quay về trang login để tránh lỗi thiếu index.html
                .logout(l -> l
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // JWT filter chạy trước Username/Password
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Lấy AuthenticationManager mặc định
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
