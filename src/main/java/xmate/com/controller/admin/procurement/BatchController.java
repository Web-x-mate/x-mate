// src/main/java/xmate/com/controller/admin/procurement/BatchAdminController.java
package xmate.com.controller.admin.procurement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.procurement.Batch;
import xmate.com.service.procurement.BatchService;

@Controller
@RequestMapping("/admin/procurement/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService service;

    @GetMapping
    public String index(@RequestParam(required = false) Long variantId,
                        @RequestParam(required = false) Long poItemId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<Batch> p = service.search(variantId, poItemId, PageRequest.of(page, size, Sort.by("receivedAt").descending().and(Sort.by("id").descending())));
        model.addAttribute("page", p);
        model.addAttribute("variantId", variantId);
        model.addAttribute("poItemId", poItemId);
        return "procurement/batches/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("batch", new Batch());
        return "procurement/batches/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Batch b, RedirectAttributes ra) {
        // chỉ cần set variant.id và poItem.id từ form
        service.create(b);
        ra.addFlashAttribute("success", "Đã tạo batch");
        return "redirect:/admin/procurement/batches";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("batch", service.get(id));
        return "procurement/batches/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Batch b, RedirectAttributes ra) {
        service.update(id, b);
        ra.addFlashAttribute("success", "Đã cập nhật batch");
        return "redirect:/admin/procurement/batches";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa batch");
        return "redirect:/admin/procurement/batches";
    }
}
