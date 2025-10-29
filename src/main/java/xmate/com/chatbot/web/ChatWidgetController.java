package xmate.com.chatbot.web;

import xmate.com.chatbot.service.ChatApiClient;
import xmate.com.dto.chatbot.ChatResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/widget/chatbot")
public class ChatWidgetController {

    private final ChatApiClient api;

    public ChatWidgetController(ChatApiClient api) {
        this.api = api;
    }

    /** Render fragment widget (gắn vào mọi page bằng Thymeleaf fragment) */
    @GetMapping
    public String widget() {
        return "widget/chatbot"; // templates/widget/chatbot.html
    }

    /** AJAX endpoint cho widget -> trả JSON (không render view) */
    @PostMapping(value = "/chat", produces = "application/json")
    @ResponseBody
    public ChatResponse ajaxChat(@RequestParam("question") String question) {
        ChatResponse res = api.chat(question);
        if (res == null) {
            res = new ChatResponse();
            res.setAnswer("Không gọi được Chat API.");
        }
        return res;
    }
}
