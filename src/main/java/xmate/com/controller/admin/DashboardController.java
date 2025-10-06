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
        if (from == null) from = to.minusDays(13);
        if (!"D".equalsIgnoreCase(gran) && !"W".equalsIgnoreCase(gran) && !"M".equalsIgnoreCase(gran)) {
            gran = "D";
        }

        String json = dashboardService.buildKpisAndSalesJson(from, to, gran);

        model.addAttribute("title", "Dashboard");
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("gran", gran);
        model.addAttribute("json", json);
        return "dashboard/index";
    }
}
