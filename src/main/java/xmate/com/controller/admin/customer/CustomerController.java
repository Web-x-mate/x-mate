// src/main/java/xmate/com/controller/admin/customer/CustomerAdminController.java
package xmate.com.controller.admin.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.customer.LoyaltyAccount;
import xmate.com.entity.customer.Segment;
import xmate.com.service.customer.CustomerService;
import xmate.com.service.customer.SegmentService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;
    private final SegmentService segmentService;

    @GetMapping
    public String index(@RequestParam(defaultValue="") String q,
                        @RequestParam(defaultValue="0") int page,
                        @RequestParam(defaultValue="10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Customer> p = service.search(q, pageable);
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("customer", new Customer());
        model.addAttribute("segments", segmentService.all());
        model.addAttribute("points", 0);
        model.addAttribute("tier", "");
        return "customers/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute Customer customer,
                         @RequestParam(required=false) Integer points,
                         @RequestParam(required=false) String tier,
                         @RequestParam(required=false, name="segmentIds") List<Long> segmentIds,
                         RedirectAttributes ra) {
        Set<Long> segIds = segmentIds != null ? new HashSet<>(segmentIds) : Collections.emptySet();
        service.create(customer, points, tier, segIds);
        ra.addFlashAttribute("success", "Tạo khách hàng thành công");
        return "redirect:/admin/customers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Customer c = service.get(id);
        // LoyaltyAccount có thể lazy; dùng optional display
        // (ở ServiceImpl update/create mình luôn đảm bảo tồn tại)
        model.addAttribute("customer", c);
        model.addAttribute("segments", segmentService.all());
        LoyaltyAccount la = c.getLoyaltyAccount();
        model.addAttribute("points", la != null ? la.getPoints() : 0);
        model.addAttribute("tier",   la != null ? la.getTier()   : "");
        model.addAttribute("selectedSegmentIds",
                c.getSegments() != null ? c.getSegments().stream().map(Segment::getId).collect(Collectors.toSet()) : Set.of());
        return "customers/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute Customer customer,
                         @RequestParam(required=false) Integer points,
                         @RequestParam(required=false) String tier,
                         @RequestParam(required=false, name="segmentIds") List<Long> segmentIds,
                         RedirectAttributes ra) {
        Set<Long> segIds = segmentIds != null ? new HashSet<>(segmentIds) : Collections.emptySet();
        service.update(id, customer, points, tier, segIds);
        ra.addFlashAttribute("success", "Cập nhật khách hàng thành công");
        return "redirect:/admin/customers";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa khách hàng");
        return "redirect:/admin/customers";
    }
}
