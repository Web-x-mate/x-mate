// src/main/java/xmate/com/controller/admin/customer/SegmentAdminController.java
package xmate.com.controller.admin.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.domain.customer.Segment;
import xmate.com.service.customer.SegmentService;

@Controller
@RequestMapping("/admin/customers/segments")
@RequiredArgsConstructor
public class SegmentController {

    private final SegmentService service;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Segment> p = service.search(q, pageable);
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "customers/segments/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("segment", new Segment());
        return "customers/segments/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Segment segment, RedirectAttributes ra) {
        service.create(segment);
        ra.addFlashAttribute("success", "Đã tạo segment");
        return "redirect:/admin/customers/segments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("segment", service.get(id));
        return "customers/segments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Segment segment, RedirectAttributes ra) {
        service.update(id, segment);
        ra.addFlashAttribute("success", "Đã cập nhật segment");
        return "redirect:/admin/customers/segments";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa segment");
        return "redirect:/admin/customers/segments";
    }

}
