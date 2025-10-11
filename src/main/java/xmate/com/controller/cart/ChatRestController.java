package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import xmate.com.repo.ChatMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatMessageRepository repo;
    private final SimpMessagingTemplate template;

    // --- DTO ---
    record MessageDto(String sender, String content, String room, LocalDateTime createdAt) {}
    record InboxItemDto(Long userId, String userEmail, String room, long time, String preview) {}

    // Lịch sử 1 phòng
    @GetMapping("/history")
    public List<MessageDto> history(@RequestParam("room") String room) {
        return repo.findByRoomOrderByCreatedAtAsc(room).stream()
                .map(e -> new MessageDto(e.getSenderEmail(), e.getContent(), e.getRoom(), e.getCreatedAt()))
                .toList();
    }

    // Inbox cho admin
    @GetMapping("/admin/inbox")
    public List<InboxItemDto> inbox() {
        return repo.findAdminInboxRows().stream()
                .map(r -> new InboxItemDto(r.getUserId(), r.getUserEmail(), r.getRoom(),
                        r.getTime().getTime(), r.getPreview()))
                .toList();
    }

    // Xoá 1 thread: admin ↔ userId
    @DeleteMapping("/thread/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> deleteThread(@PathVariable Long userId) {
        String room = "u-" + userId;
        int n = repo.deleteByRoom(room);

        // Báo cho client đang mở phòng đó
        template.convertAndSend("/topic/room." + room,
                Map.of("system", true, "event", "cleared", "room", room, "count", n));

        // Gợi ý client refresh inbox
        template.convertAndSend("/topic/admin.inbox",
                Map.of("system", true, "event", "inbox_reload"));

        return Map.of("deleted", n, "room", room);
    }

    // Xoá tất cả thread kiểu u-{id}
    @DeleteMapping("/threads")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> deleteAllAdminThreads() {
        int n = repo.deleteAllAdminThreads();

        template.convertAndSend("/topic/admin.inbox",
                Map.of("system", true, "event", "inbox_reload"));

        return Map.of("deleted", n);
    }
}
