package xmate.com.controller.admin.customer;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.customer.Customer;
import xmate.com.service.customer.AdminCustomerService;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    private final AdminCustomerService service;

    public CustomerController (AdminCustomerService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        Pageable pageable = PageRequest.of(page, size);
        var p = service.search(q, pageable);
        model.addAttribute("q", q);
        model.addAttribute("page", p);
        model.addAttribute("items", p.getContent());
        return "customers/customer/list";
    }
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("c", new Customer());
        return "customers/customer/form";
    }
    @PreAuthorize("hasAuthority('CUSTOMER_EDIT')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var c = service.get(id);
        if (c == null) {
            ra.addFlashAttribute("error", "Không tìm thấy khách hàng");
            return "redirect:/admin/customers";
        }
        model.addAttribute("c", c);
        return "customers/customer/form";
    }

    @PostMapping
    public String save(@ModelAttribute("c") Customer c,
                       RedirectAttributes ra) {
        try {
            // Nếu bạn không muốn cho sửa email sau khi tạo, hãy làm read-only ở view.
            service.save(c);
            ra.addFlashAttribute("saved", true);
            return "redirect:/admin/customers";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (c.getId() == null) return "redirect:/admin/customers/new";
            return "redirect:/admin/customers/" + c.getId() + "/edit";
        }
    }
    @PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            service.delete(id);
            ra.addFlashAttribute("deleted", true);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/customers";
    }
}
