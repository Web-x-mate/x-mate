// src/main/java/xmate/com/controller/admin/procurement/SupplierAdminController.java
package xmate.com.controller.admin.procurement;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.procurement.Supplier;
import xmate.com.service.procurement.SupplierService;

@Controller
@RequestMapping("/admin/procurement/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService service;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<Supplier> p = service.search(q, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "procurement/suppliers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "procurement/suppliers/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Supplier s, RedirectAttributes ra) {
        service.create(s);
        ra.addFlashAttribute("success", "Đã thêm nhà cung cấp");
        return "redirect:/admin/procurement/suppliers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", service.get(id));
        return "procurement/suppliers/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Supplier s, RedirectAttributes ra) {
        service.update(id, s);
        ra.addFlashAttribute("success", "Đã cập nhật nhà cung cấp");
        return "redirect:/admin/procurement/suppliers";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa");
        return "redirect:/admin/procurement/suppliers";
    }
}
