package xmate.com.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry reg) {
        reg.addMapping("/api/**")
                .allowedOriginPatterns(               // dùng patterns thay vì "*"
                        "http://localhost:8080",
                        "http://localhost:3000",
                        "https://gaye-swampy-genetically.ngrok-free.dev",
                        "https://your-frontend.vercel.app"  // deploy FE sau này
                )
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
