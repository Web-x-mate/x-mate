package xmate.com.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.*;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình springdoc-openapi (Swagger UI tại /swagger-ui/index.html).
 *
 * Dependencies (pom.xml):
 *  <dependency>
 *    <groupId>org.springdoc</groupId>
 *    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
 *    <version>2.5.0</version>
 *  </dependency>
 */
@Configuration
public class SwaggerConfig {

    // Nhóm API cho các endpoint /api/**
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("xmate-api")
                .pathsToMatch("/api/**")
                .build();
    }

    // OpenAPI metadata + (tuỳ chọn) Bearer auth cho REST API
    @Bean
    public OpenAPI xmateOpenAPI() {
        final String bearerKey = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("X-mate Admin API")
                        .description("Tài liệu OpenAPI cho các endpoint REST (/api/**)")
                        .version("v1.0.0")
                        .contact(new Contact().name("X-mate Team").email("support@xmate.local"))
                        .license(new License().name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes(bearerKey,
                                new SecurityScheme()
                                        .name(bearerKey)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                // Nếu bạn dùng JWT cho /api/**, bật dòng dưới để yêu cầu auth ở Swagger:
                //.addSecurityItem(new SecurityRequirement().addList(bearerKey))
                ;
    }
}
