package xmate.com.controller.admin.sales;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import xmate.com.service.sales.PaymentService;

@Controller
@RequestMapping("/admin/payments")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final PaymentService paymentService;

    @GetMapping("/pending")
    public String pending(Model model) {
        model.addAttribute("items", paymentService.listPendingProofs());
        return "admin/payments_pending";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal) {
        paymentService.approveProof(id, principal.getName());
        return "redirect:/admin/payments/pending?ok";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(value = "note", required = false) String note,
                         Principal principal) {
        paymentService.rejectProof(id, principal.getName(), note);
        return "redirect:/admin/payments/pending?rejected";
    }
}
