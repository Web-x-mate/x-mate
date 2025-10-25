
package xmate.com.chatbot.service;

import xmate.com.dto.chatbot.ChatRequest;
import xmate.com.dto.chatbot.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class ChatApiClient {
    private final WebClient client;

    public ChatApiClient(WebClient shopbotClient) {
        this.client = shopbotClient;
    }

    public ChatResponse chat(String question) {
        ChatRequest req = new ChatRequest(question);
        return client.post()
                .uri("/chat") // theo docs#/chat/chat_chat_post
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .onErrorResume(ex -> {
                    ChatResponse fallback = new ChatResponse();
                    fallback.setAnswer("Không gọi được FastAPI: " + ex.getMessage());
                    return Mono.just(fallback);
                })
                .block();
    }
}
