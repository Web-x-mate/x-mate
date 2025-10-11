// src/main/java/xmate/com/controller/admin/system/ActivityLogAdminController.java
package xmate.com.controller.admin.system;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import xmate.com.entity.system.ActivityLog;
import xmate.com.service.system.ActivityLogService;

@Controller
@RequestMapping("/admin/system/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {
    private final ActivityLogService logService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) String entityType,
                        @RequestParam(required = false) Long entityId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<ActivityLog> p;
        if (entityType != null || entityId != null) {
            p = logService.byEntity(entityType, entityId, pageable);
        } else {
            p = logService.search(q, pageable);
        }
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        model.addAttribute("entityType", entityType);
        model.addAttribute("entityId", entityId);
        return "system/activity-logs/list";
    }
}
