// src/main/java/xmate/com/config/SecurityConfig.java
package xmate.com.config;

import jakarta.servlet.http.HttpServletResponse;
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

                // Dùng session nếu cần cho form-login / oauth2
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // Phân quyền
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/error",
                                "/auth/**", "/api/auth/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico", "/ws/**",
                                "/uploads/**" // ảnh biên lai
                        ).permitAll()

                        // bắt buộc đăng nhập cho checkout APIs
                        .requestMatchers("/api/checkout/**").authenticated()

                        // upload biên lai: để authenticated (đổi thành permitAll nếu bạn muốn cho khách chưa login cũng gửi)
                        .requestMatchers("/api/payment/proof").permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().permitAll()
                )

                // Trả 401 khi chưa đăng nhập để FE bắt được
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)
                ))

                .authenticationProvider(daoAuthProvider)

                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(roleRedirect)
                        .failureUrl("/auth/login?error")
                        .permitAll()
                )

                .oauth2Login(o -> o
                        .loginPage("/auth/login")
                        .userInfoEndpoint(ui -> ui.userService(oAuth2UserService))
                        .successHandler(roleRedirect)
                        .failureHandler((rq, rs, ex) -> { ex.printStackTrace(); rs.sendRedirect("/auth/login?oauth2Error"); })
                )

                .logout(l -> l
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // JWT filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
