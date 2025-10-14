// src/main/java/xmate/com/controller/admin/MembershipTierAdminController.java
package xmate.com.controller.admin.customer;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.customer.MembershipTier;
import xmate.com.service.customer.MembershipTierAdminService;

@Controller
@RequestMapping("/admin/membership-tiers")
public class MembershipTierAdminController {

    private final MembershipTierAdminService service;

    public MembershipTierAdminController(MembershipTierAdminService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size);
        var p = service.list(pageable);
        model.addAttribute("page", p);
        model.addAttribute("items", p.getContent());
        return "customers/membershipTier/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("t", new MembershipTier());
        return "customers/membershipTier/form";
    }

    @GetMapping("/{code}/edit")
    public String editForm(@PathVariable String code, Model model, RedirectAttributes ra) {
        var t = service.get(code);
        if (t == null) {
            ra.addFlashAttribute("error", "Không tìm thấy tier");
            return "redirect:/admin/membership-tiers";
        }
        model.addAttribute("t", t);
        return "customers/membershipTier/form";
    }

    @PostMapping
    public String save(@ModelAttribute("t") MembershipTier t,
                       RedirectAttributes ra) {
        try {
            service.save(t);
            ra.addFlashAttribute("saved", true);
            return "redirect:/admin/membership-tiers";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (t.getCode() == null || t.getCode().isBlank()) return "redirect:/admin/membership-tiers/new";
            return "redirect:/admin/membership-tiers/" + t.getCode() + "/edit";
        }
    }

    @PostMapping("/{code}/delete")
    public String delete(@PathVariable String code, RedirectAttributes ra) {
        try {
            service.delete(code);
            ra.addFlashAttribute("deleted", true);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/membership-tiers";
    }
}
