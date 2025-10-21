// src/main/java/xmate/com/config/WebConfig.java
package xmate.com.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Redirect "/" về trang đăng nhập để tránh Thymeleaf đòi index.html */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Nếu trước đây có: registry.addViewController("/").setViewName("index"); -> hãy xoá bỏ
        registry.addRedirectViewController("/", "/auth/login");
    }

    /** Cho phép truy cập http://host/uploads/** tới thư mục ./uploads */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
