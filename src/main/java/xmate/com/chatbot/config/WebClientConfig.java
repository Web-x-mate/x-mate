
package xmate.com.chatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient shopbotClient(@Value("${shopbot.base-url:http://127.0.0.1:8000}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
