// src/main/java/xmate/com/config/SecurityConfig.java
package xmate.com.config;

import lombok.RequiredArgsConstructor;
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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtFilter;
    private final AuthenticationProvider daoAuthProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(c -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // static
                        .requestMatchers("/", "/css/**","/js/**","/images/**").permitAll()

                        // AUTH endpoints (public)
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/google",
                                "/api/auth/facebook"
                        ).permitAll()
                        .requestMatchers("/auth/login","/auth/register","/auth/forgot").permitAll()

                        // Các trang web yêu cầu đã đăng nhập bằng JWT
                        .requestMatchers("/auth/complete/**").authenticated()
                        .requestMatchers("/user/**").authenticated()

                        // API bảo vệ bởi JWT
                        .requestMatchers("/api/**").authenticated()

                        // còn lại cho phép
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
