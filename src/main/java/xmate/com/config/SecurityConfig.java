package xmate.com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().permitAll()
                )
                .authenticationProvider(daoAuthProvider) // 👈 ĐĂNG KÝ provider CHUẨN, KHÔNG vòng lặp
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
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // Dùng AuthenticationConfiguration để lấy AuthenticationManager mặc định (ProviderManager)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}

