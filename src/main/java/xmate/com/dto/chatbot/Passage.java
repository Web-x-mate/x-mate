package xmate.com.dto.chatbot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Passage {
    @JsonProperty("product_id")
    private Integer productId;

    private String type;
    private Double score;
    private String snippet;

    private String slug; // optional
    private String url;  // optional
}
