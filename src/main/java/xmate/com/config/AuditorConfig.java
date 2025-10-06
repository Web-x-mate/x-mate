package xmate.com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Không dùng Security → không xác định được user hiện tại.
 * Trả Optional.empty() để JPA không tự set createdBy/updatedBy.
 * (Hoặc bạn có thể return Optional.of(0L) nếu muốn mặc định 0.)
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditorConfig {

    @Bean
    public AuditorAware<Long> auditorProvider() {
        return Optional::empty; // hoặc: () -> Optional.of(0L)
    }
}
