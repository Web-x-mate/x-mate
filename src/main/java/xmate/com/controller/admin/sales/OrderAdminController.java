// src/main/java/xmate/com/controller/admin/sales/OrderAdminController.java
package xmate.com.controller.admin.sales;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.sales.OrderService;

// 🔁 Dùng enums đúng package
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/sales/orders")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService service;
    private final CustomerRepository customerRepo;
    private final ProductVariantRepository variantRepo;

    /** Nếu price để 0 ở form → tự lấy giá gốc từ Variant; đồng thời set lại lineTotal. */
    private void ensureDefaultPriceIfNull(List<OrderItem> items) {
        if (items == null) return;
        for (OrderItem it : items) {
            if (it == null || it.getVariant() == null || it.getVariant().getId() == null) continue;

            // Price trong OrderItem là long → coi 0 là "chưa nhập" để auto-fill
            if (it.getPrice() == 0L) {
                var v = variantRepo.findById(it.getVariant().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + it.getVariant().getId()));
                it.setPrice(priceToLong(v.getPrice())); // convert BigDecimal/Long → long
            }
            if (it.getQty() < 0) it.setQty(0);

            // lineTotal = price * qty
            it.setLineTotal(it.getPrice() * (long) it.getQty());
        }
    }

    @GetMapping
    public String index(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String payment,
                        @RequestParam(required = false) String shipping,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Order> p = service.search(q, status, payment, shipping, pageable);

        model.addAttribute("page", p);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("payment", payment);
        model.addAttribute("shipping", shipping);

        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("shippingStatuses", getShippingStatuses()); // List<String>

        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new OrderForm());
        model.addAttribute("customers", getAllCustomers());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("shippingStatuses", getShippingStatuses());
        return "orders/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") OrderForm form, RedirectAttributes ra) {
        Order order = form.toOrder();
        List<OrderItem> items = form.toItems();
        ensureDefaultPriceIfNull(items);
        service.create(order, items);
        ra.addFlashAttribute("success", "Tạo đơn hàng thành công");
        return "redirect:/admin/sales/orders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Order o = service.get(id);
        model.addAttribute("form", OrderForm.fromOrder(o));
        model.addAttribute("customers", getAllCustomers());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("shippingStatuses", getShippingStatuses());
        return "orders/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") OrderForm form, RedirectAttributes ra) {
        List<OrderItem> items = form.toItems();
        ensureDefaultPriceIfNull(items);
        service.update(id, form.toOrder(), items);
        ra.addFlashAttribute("success", "Cập nhật đơn hàng thành công");
        return "redirect:/admin/sales/orders";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Đã xóa đơn hàng");
        return "redirect:/admin/sales/orders";
    }

    private List<Customer> getAllCustomers() {
        return customerRepo.findAll(PageRequest.of(0, 200, Sort.by("id").descending())).getContent();
    }

    /** Tập giá trị shippingStatus dạng String để hiển thị trong form (tạm thời chưa làm enum). */
    private List<String> getShippingStatuses() {
        return Arrays.asList(
                "NOT_SHIPPED",
                "PICKING",
                "SHIPPING",
                "DELIVERED",
                "RETURNED",
                "CANCELLED"
        );
    }

    // ======= DTO FORM (đồng bộ long VND) =======
    @Data
    public static class OrderForm {
        private Long id;
        private Long customerId;
        private String code;
        private String shippingAddress;
        private String shippingProvider;
        private String trackingCode;
        private String noteInternal;

        // ✅ Dùng enums đúng package
        private OrderStatus status = OrderStatus.PENDING_PAYMENT;
        private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

        // ✅ shippingStatus là String để khớp với Entity Order
        private String shippingStatus = "NOT_SHIPPED";

        private long discountAmount = 0L;
        private long shippingFee = 0L;

        // items (parallel arrays)
        private List<Long> variantIds = new ArrayList<>();
        private List<Long> prices = new ArrayList<>();
        private List<Integer> qtys = new ArrayList<>();

        public Order toOrder() {
            Order o = new Order();
            o.setId(id);
            if (customerId != null) {
                Customer c = new Customer();
                c.setId(customerId);
                o.setCustomer(c);
            }
            o.setCode(code);
            o.setShippingAddress(shippingAddress);
            o.setShippingProvider(shippingProvider);
            o.setTrackingCode(trackingCode);
            o.setNoteInternal(noteInternal);

            o.setStatus(status);                 // enum
            o.setPaymentStatus(paymentStatus);   // enum
            o.setShippingStatus(shippingStatus); // String

            o.setDiscountAmount(discountAmount);
            o.setShippingFee(shippingFee);
            // subtotal/total sẽ được service tính lại từ items
            return o;
        }

        public List<OrderItem> toItems() {
            List<OrderItem> list = new ArrayList<>();
            for (int i = 0; i < variantIds.size(); i++) {
                Long vid = variantIds.get(i);
                if (vid == null) continue;

                long price = (i < prices.size() && prices.get(i) != null) ? prices.get(i) : 0L;
                int qty = (i < qtys.size() && qtys.get(i) != null) ? qtys.get(i) : 0;

                ProductVariant v = new ProductVariant();
                v.setId(vid);

                OrderItem it = new OrderItem();
                it.setVariant(v);
                it.setPrice(price);
                it.setQty(qty);
                it.setLineTotal(price * (long) qty);
                list.add(it);
            }
            return list;
        }

        public static OrderForm fromOrder(Order o) {
            OrderForm f = new OrderForm();
            f.setId(o.getId());
            f.setCustomerId(o.getCustomer() != null ? o.getCustomer().getId() : null);
            f.setCode(o.getCode());
            f.setShippingAddress(o.getShippingAddress());
            f.setShippingProvider(o.getShippingProvider());
            f.setTrackingCode(o.getTrackingCode());
            f.setNoteInternal(o.getNoteInternal());

            f.setStatus(o.getStatus());
            f.setPaymentStatus(o.getPaymentStatus());
            f.setShippingStatus(o.getShippingStatus()); // String

            f.setDiscountAmount(o.getDiscountAmount());
            f.setShippingFee(o.getShippingFee());

            if (o.getItems() != null) {
                for (OrderItem it : o.getItems()) {
                    f.getVariantIds().add(it.getVariant() != null ? it.getVariant().getId() : null);
                    f.getPrices().add(it.getPrice());
                    f.getQtys().add(it.getQty());
                }
            }
            return f;
        }
    }

    // ===== Helper: convert giá variant sang long
    private long priceToLong(Object priceObj) {
        if (priceObj == null) return 0L;
        if (priceObj instanceof Long l) return l;
        if (priceObj instanceof Integer i) return i.longValue();
        if (priceObj instanceof BigDecimal bd) return bd.longValue();
        if (priceObj instanceof String s) {
            try { return new BigDecimal(s).longValue(); } catch (Exception ignored) {}
        }
        return 0L;
    }
}
