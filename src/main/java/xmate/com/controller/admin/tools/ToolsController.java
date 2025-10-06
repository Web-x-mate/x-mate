/**
 * Web layer (controllers) khớp với menu bạn chốt.
 * Chỉ xử lý hiển thị (list/detail). CRUD sẽ thêm sau.
 * View name trả về cần tạo tương ứng trong resources/templates (bước UI sau).
 */

package xmate.com.controller.admin.tools;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Tools > Import/Export */
@Controller
@RequestMapping("/admin/tools")
public class ToolsController {

    @GetMapping("/import-export")
    public String importExport(Model model){
        model.addAttribute("pageTitle", "Tools - Import/Export");
        return "tools/import-export"; // templates/tools/import-export.html
    }
}
