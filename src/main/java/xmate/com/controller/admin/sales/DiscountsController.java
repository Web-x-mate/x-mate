// src/main/java/xmate/com/controller/admin/discount/DiscountAdminController.java
package xmate.com.controller.admin.sales;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.discount.Discount;
import xmate.com.service.discount.DiscountService;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@RequestMapping("/admin/discounts")
@RequiredArgsConstructor
public class DiscountsController {

    private final DiscountService service;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Discount> p = service.search(q, pageable);
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "discounts/list";
    }
    @PreAuthorize("hasAnyAuthority('DISCOUNT_CREATE','ROLE_ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("discount", new Discount());
        return "discounts/form";
    }
    @PreAuthorize("hasAnyAuthority('DISCOUNT_CREATE','ROLE_ADMIN')")
    @PostMapping("/new")
    public String create(@ModelAttribute("discount") Discount d, RedirectAttributes ra) {
        service.create(d);
        ra.addFlashAttribute("success", "Tạo discount thành công");
        return "redirect:/admin/discounts";
    }
    @PreAuthorize("hasAnyAuthority('DISCOUNT_EDIT','ROLE_ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("discount", service.get(id));
        return "discounts/form";
    }
    @PreAuthorize("hasAnyAuthority('DISCOUNT_EDIT','ROLE_ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("discount") Discount d, RedirectAttributes ra) {
        service.update(id, d);
        ra.addFlashAttribute("success", "Cập nhật discount thành công");
        return "redirect:/admin/discounts";
    }
    @PreAuthorize("hasAnyAuthority('DISCOUNT_DELETE','ROLE_ADMIN')")
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xoá discount");
        return "redirect:/admin/discounts";
    }
}
