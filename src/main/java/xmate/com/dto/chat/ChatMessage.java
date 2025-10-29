package xmate.com.dto.chat;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    private String room;      // "u-{userId}"
    private String from;      // email người gửi
    private String content;   // nội dung
    private long   time;      // timestamp
}
