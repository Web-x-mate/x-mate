// src/main/java/xmate/com/controller/admin/DashboardController.java
package xmate.com.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import xmate.com.service.dashboard.DashboardService;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({ "", "/dashboard" })
    public String page(Model model,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false, defaultValue = "D") String gran) {

        LocalDate today = LocalDate.now();
        if (to == null) to = today;
        // Mặc định 30 ngày để biểu đồ có đủ dữ liệu nhưng không quá dày
        if (from == null) from = to.minusDays(29);

        // Chuẩn hoá gran cho UI (D/W/M) và cho repository (DAY/MONTH/YEAR)
        String uiGran = (gran == null ? "D" : gran.trim().toUpperCase());
        if (!("D".equals(uiGran) || "W".equals(uiGran) || "M".equals(uiGran))) uiGran = "D";

        // Repo hiện hỗ trợ DAY/MONTH/YEAR. Với W(week) ta dùng DAY rồi frontend sẽ gộp tuần để hiển thị.
        String repoGran = switch (uiGran) {
            case "M" -> "MONTH";
            case "W" -> "DAY";   // tuần sẽ nhóm ở phía client
            default -> "DAY";      // D
        };

        String json = dashboardService.buildKpisAndSalesJson(from, to, repoGran);

        model.addAttribute("title", "Dashboard");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("gran", uiGran);
        model.addAttribute("json", json);
        return "dashboard/index";
    }
}
