package xmate.com.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatRequest {
    @JsonProperty("question")
    private String question;

    public ChatRequest() {}
    public ChatRequest(String question) { this.question = question; }
}
