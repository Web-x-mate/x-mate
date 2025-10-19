// src/main/java/xmate/com/controller/admin/procurement/PurchaseOrderAdminController.java
package xmate.com.controller.admin.procurement;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.entity.common.POStatus;
import xmate.com.entity.procurement.PurchaseOrder;
import xmate.com.entity.procurement.PurchaseOrderItem;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.procurement.Supplier;
import xmate.com.service.procurement.PurchaseOrderService;
import xmate.com.service.procurement.SupplierService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/procurement/po")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService service;
    private final SupplierService supplierService;

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) POStatus status,
                        @RequestParam(required = false) Long supplierId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Page<PurchaseOrder> p = service.search(q, status, supplierId, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("page", p);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("supplierId", supplierId);
        model.addAttribute("statuses", POStatus.values());
        model.addAttribute("suppliers", supplierService.search("", PageRequest.of(0, 200)).getContent());
        return "procurement/po/list";
    }
    @PreAuthorize("hasAnyAuthority('PO_CREATE','ROLE_ADMIN')")
    @GetMapping("/new")
    public String createForm(Model model) {
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.getVariantIds().add(null); // 1 dòng trống
        form.getQtys().add(0);
        form.getCosts().add(BigDecimal.ZERO);
        form.getReceivedQtys().add(0);

        model.addAttribute("form", form);
        model.addAttribute("statuses", POStatus.values());
        model.addAttribute("suppliers", supplierService.search("", PageRequest.of(0, 200)).getContent());
        return "procurement/po/form";
    }
    @PreAuthorize("hasAnyAuthority('PO_CREATE','ROLE_ADMIN')")
    @PostMapping("/new")
    public String create(@ModelAttribute("form") PurchaseOrderForm form, RedirectAttributes ra) {
        PurchaseOrder po = form.toPO();
        List<PurchaseOrderItem> items = form.toItems();
        service.create(po, items);
        ra.addFlashAttribute("success", "Đã tạo PO");
        return "redirect:/admin/procurement/po";
    }
    @PreAuthorize("hasAnyAuthority('PO_EDIT','ROLE_ADMIN')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PurchaseOrder po = service.get(id);
        model.addAttribute("form", PurchaseOrderForm.from(po, service.itemsOf(id)));
        model.addAttribute("statuses", POStatus.values());
        model.addAttribute("suppliers", supplierService.search("", PageRequest.of(0, 200)).getContent());
        return "procurement/po/form";
    }
    @PreAuthorize("hasAnyAuthority('PO_EDIT','ROLE_ADMIN')")
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") PurchaseOrderForm form, RedirectAttributes ra) {
        service.update(id, form.toPO(), form.toItems());
        ra.addFlashAttribute("success", "Đã cập nhật PO");
        return "redirect:/admin/procurement/po";
    }
    @PreAuthorize("hasAnyAuthority('PO_DELETE','ROLE_ADMIN')")
    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa PO");
        return "redirect:/admin/procurement/po";
    }

    @PreAuthorize("hasAnyAuthority('PO_STATUS_UPDATE','ROLE_ADMIN')")
    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam POStatus status, RedirectAttributes ra) {
        service.changeStatus(id, status);
        ra.addFlashAttribute("success", "Đã đổi trạng thái");
        return "redirect:/admin/procurement/po";
    }

    // ====== Form DTO ======
    @Data
    public static class PurchaseOrderForm {
        private Long id;
        private String code;
        private Long supplierId;
        private POStatus status = POStatus.DRAFT;
        private LocalDate expectedDate;

        // items
        private List<Long> variantIds = new ArrayList<>();
        private List<Integer> qtys = new ArrayList<>();
        private List<BigDecimal> costs = new ArrayList<>();
        private List<Integer> receivedQtys = new ArrayList<>();

        public PurchaseOrder toPO() {
            PurchaseOrder p = new PurchaseOrder();
            p.setId(id);
            p.setCode(code);
            if (supplierId != null) {
                Supplier s = new Supplier();
                s.setId(supplierId);
                p.setSupplier(s);
            }
            p.setStatus(status);
            p.setExpectedDate(expectedDate);
            return p;
        }

        public List<PurchaseOrderItem> toItems() {
            List<PurchaseOrderItem> list = new ArrayList<>();
            int n = variantIds != null ? variantIds.size() : 0;
            for (int i = 0; i < n; i++) {
                Long vid = variantIds.get(i);
                if (vid == null) continue;
                Integer qty = (i < qtys.size() && qtys.get(i) != null) ? qtys.get(i) : 0;
                BigDecimal cost = (i < costs.size() && costs.get(i) != null) ? costs.get(i) : BigDecimal.ZERO;
                Integer rcv = (i < receivedQtys.size() && receivedQtys.get(i) != null) ? receivedQtys.get(i) : 0;

                ProductVariant v = new ProductVariant();
                v.setId(vid);

                PurchaseOrderItem it = new PurchaseOrderItem();
                it.setVariant(v);
                it.setQty(qty);
                it.setCost(cost);
                it.setReceivedQty(rcv);
                list.add(it);
            }
            return list;
        }

        public static PurchaseOrderForm from(PurchaseOrder po, List<PurchaseOrderItem> items) {
            PurchaseOrderForm f = new PurchaseOrderForm();
            f.setId(po.getId());
            f.setCode(po.getCode());
            f.setSupplierId(po.getSupplier() != null ? po.getSupplier().getId() : null);
            f.setStatus(po.getStatus());
            f.setExpectedDate(po.getExpectedDate());
            if (items != null) {
                for (PurchaseOrderItem it : items) {
                    f.variantIds.add(it.getVariant() != null ? it.getVariant().getId() : null);
                    f.qtys.add(it.getQty());
                    f.costs.add(it.getCost());
                    f.receivedQtys.add(it.getReceivedQty());
                }
            }
            return f;
        }
    }
}
