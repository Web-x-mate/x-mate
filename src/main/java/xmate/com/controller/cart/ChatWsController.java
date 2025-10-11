package xmate.com.controller.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import xmate.com.entity.ChatMessageEntity;
import xmate.com.repo.ChatMessageRepository;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final SimpMessagingTemplate template;
    private final ChatMessageRepository repo;

    @MessageMapping("/chat.send/{room}")
    public void send(@DestinationVariable String room, ChatMessage m) {
        // 1) Lưu DB
        ChatMessageEntity e = new ChatMessageEntity();
        e.setRoom(room);
        e.setSenderId(m.getSenderId());
        e.setSenderEmail(m.getSender());
        e.setContent(m.getContent());
        e.setCreatedAt(LocalDateTime.now());
        repo.save(e);

        // 2) Phát về phòng
        template.convertAndSend("/topic/room." + room, m);

        // 3) Gửi notification cho admin nếu là user thật (senderId > 0)
        if (m.getSenderId() != null && m.getSenderId() > 0) {
            Long userId = parseUserId(room);
            template.convertAndSend("/topic/admin.inbox",
                    new AdminNotif(
                            userId,
                            m.getSender(),
                            m.getContent(),
                            room,
                            System.currentTimeMillis()
                    )
            );
        }
    }

    private Long parseUserId(String room) {
        try {
            int pos = room.indexOf('-');
            return pos >= 0 ? Long.valueOf(room.substring(pos + 1)) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ChatMessage {
        private Long senderId;
        private String sender;
        private String content;
        private String room;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AdminNotif {
        private Long userId;
        private String userEmail;
        private String preview;
        private String room;
        private long time;
    }
}
