// src/main/java/xmate/com/controller/admin/inventory/InventoryAdminController.java
package xmate.com.controller.admin.inventory;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.domain.common.InventoryRefType;
import xmate.com.domain.inventory.Inventory;
import xmate.com.service.inventory.InventoryService;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Page<Inventory> p = service.searchStock(q, PageRequest.of(page, size, Sort.by("variantId").ascending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "inventory/list";
    }

    @GetMapping("/adjust/{variantId}")
    public String adjustForm(@PathVariable Long variantId, Model model) {
        Inventory i = service.getOrCreate(variantId);
        model.addAttribute("inv", i);
        model.addAttribute("form", new AdjustForm());
        model.addAttribute("refTypes", InventoryRefType.values());
        return "inventory/adjust";
    }

    @PostMapping("/adjust/{variantId}")
    public String doAdjust(@PathVariable Long variantId,
                           @ModelAttribute("form") AdjustForm form,
                           RedirectAttributes ra) {
        if (form.delta == null) form.delta = 0;
        // lưu ý: tên hàm service phải khớp. Nếu service bạn là `adjust(...)` thì gọi đúng tên.
        service.adjustOnHand(variantId, form.delta, form.refType, form.refId, form.note, form.userId);
        ra.addFlashAttribute("success", "Đã điều chỉnh tồn kho");
        return "redirect:/admin/inventory";
    }

    @Data
    public static class AdjustForm {
        private Integer delta; // + nhập kho, - xuất/huỷ
        private InventoryRefType refType;
        private Long refId;
        private String note;
        private Long userId;
    }
}
