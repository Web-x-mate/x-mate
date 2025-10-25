package xmate.com.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String intent;
    private String answer;

    @JsonProperty("tool_json")
    private Map<String, Object> toolJson;

    private List<Passage> passages;
}
