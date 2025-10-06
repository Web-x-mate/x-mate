/**
 * Web layer (controllers) khớp với menu bạn chốt.
 * Chỉ xử lý hiển thị (list/detail). CRUD sẽ thêm sau.
 * View name trả về cần tạo tương ứng trong resources/templates (bước UI sau).
 */

package xmate.com.controller.admin.settings;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Settings > Quản lý thông tin cá nhân */
@Controller
@RequestMapping("/admin/settings")
public class SettingsController {

    @GetMapping("/profile")
    public String profile(Model model){
        model.addAttribute("pageTitle", "Settings - Profile");
        return "settings/profile"; // templates/settings/profile.html
    }
}
