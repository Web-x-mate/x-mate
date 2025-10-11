package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.util.SecurityUtil;

@Controller
@RequiredArgsConstructor
public class ChatPageController {

    private final CustomerRepository userRepo;
    private final SecurityUtil securityUtil;
    /** ==================== ADMIN ==================== **/
    // 🔹 Trang inbox realtime để admin xem ai vừa nhắn (giống Messenger)
    @GetMapping("/admin/inbox")
    public String adminInbox(Model model) {
        model.addAttribute("me", SecurityUtil.currentEmail());
        return "chat/admin-inbox"; // View: templates/chat/admin-inbox.html
    }

    // 🔹 Admin mở chat với khách bằng email (cũ)
    @GetMapping(value = "/admin/chat", params = "userEmail")
    public String chatAdmin(@RequestParam("userEmail") String userEmail, Model model) {
        String adminEmail = SecurityUtil.currentEmail();
        model.addAttribute("me", adminEmail);
        model.addAttribute("isAdmin", true);

        Customer target = userRepo.findByEmail(userEmail).orElse(null);
        if (target == null) {
            model.addAttribute("error", "Không tìm thấy user với email: " + userEmail);
            return "chat/error";
        }

        String room = "u-" + target.getId();
        model.addAttribute("room", room);
        model.addAttribute("peerEmail", target.getEmail());
        model.addAttribute("meId", securityUtil.currentUserId());
        return "chat/user";
    }

    // 🔹 Admin mở chat với khách bằng userId (an toàn hơn)
    @GetMapping(value = "/admin/chat", params = "userId")
    public String chatAdminById(@RequestParam("userId") Long userId, Model model) {
        String adminEmail = SecurityUtil.currentEmail();
        model.addAttribute("me", adminEmail);
        model.addAttribute("isAdmin", true);

        var target = userRepo.findById(userId).orElse(null);
        if (target == null) {
            model.addAttribute("error", "Không tìm thấy user id=" + userId);
            return "chat/error";
        }

        // Phòng quy ước admin<->user
        String room = "u-" + target.getId();
        model.addAttribute("room", room);
        model.addAttribute("peerEmail", target.getEmail());
        model.addAttribute("meId", securityUtil.currentUserId());
        return "chat/user";
    }

    /** ==================== USER ==================== **/

    // 🔹 Người dùng vào phòng chat của chính mình (với Admin)
    @GetMapping("/chat")
    public String chatUser(Model model) {
        String meEmail = SecurityUtil.currentEmail();
        model.addAttribute("me", meEmail);
        model.addAttribute("isAdmin", false);

        Customer me = userRepo.findByEmail(meEmail).orElse(null);
        if (me == null) {
            model.addAttribute("error", "Không tìm thấy user hiện tại!");
            return "chat/error";
        }

        String room = "u-" + me.getId(); // mỗi user có một phòng riêng
        model.addAttribute("room", room);
        model.addAttribute("peerEmail", "Admin");
        model.addAttribute("meId", me.getId());
        return "chat/user";
    }

    // 🔹 Người dùng mở chat trực tiếp với người khác (user ↔ user)
    @GetMapping("/chat/with")
    public String chatWithPeer(@RequestParam("email") String peerEmail, Model model) {
        String meEmail = SecurityUtil.currentEmail();
        model.addAttribute("me", meEmail);
        model.addAttribute("isAdmin", false);

        var me   = userRepo.findByEmail(meEmail).orElse(null);
        var peer = userRepo.findByEmail(peerEmail).orElse(null);
        if (me == null || peer == null) {
            model.addAttribute("error", "Không tìm thấy người dùng đích: " + peerEmail);
            return "chat/error";
        }

        long a = Math.min(me.getId(), peer.getId());
        long b = Math.max(me.getId(), peer.getId());
        String room = "u-" + a + "_" + b;   // VD: u-9_12

        model.addAttribute("room", room);
        model.addAttribute("peerEmail", peer.getEmail());
        model.addAttribute("meId", me.getId());
        return "chat/user";
    }
}
