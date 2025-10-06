// src/main/java/xmate/com/controller/admin/sales/OrderAdminController.java
package xmate.com.controller.admin.sales;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.domain.catalog.ProductVariant;
import xmate.com.domain.common.OrderStatus;
import xmate.com.domain.common.PaymentStatus;
import xmate.com.domain.common.ShippingStatus;
import xmate.com.domain.customer.Customer;
import xmate.com.domain.sales.Order;
import xmate.com.domain.sales.OrderItem;
import xmate.com.repo.catalog.ProductVariantRepository;    // đổi sang .repository nếu dự án bạn dùng package đó
import xmate.com.repo.customer.CustomerRepository;       // đổi sang .repository nếu dự án bạn dùng package đó
import xmate.com.service.sales.OrderService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/sales/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;
    private final CustomerRepository customerRepo;
    private final ProductVariantRepository variantRepo;

    /** Nếu price bị null ở form => tự lấy giá gốc từ Variant; đồng thời set lại total. */
    private void ensureDefaultPriceIfNull(List<OrderItem> items) {
        if (items == null) return;
        for (OrderItem it : items) {
            if (it == null || it.getVariant() == null || it.getVariant().getId() == null) continue;

            if (it.getPrice() == null) {
                var v = variantRepo.findById(it.getVariant().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + it.getVariant().getId()));
                it.setPrice(v.getPrice());
            }
            if (it.getQty() == null) it.setQty(0);
            it.setTotal(it.getPrice().multiply(BigDecimal.valueOf(it.getQty())));
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
        model.addAttribute("shippingStatuses", ShippingStatus.values());
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new OrderForm());
        model.addAttribute("customers", getAllCustomers());
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("shippingStatuses", ShippingStatus.values());
        return "orders/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") OrderForm form, RedirectAttributes ra) {
        Order order = form.toOrder();
        List<OrderItem> items = form.toItems();
        ensureDefaultPriceIfNull(items);              // << auto fill giá gốc nếu để trống
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
        model.addAttribute("shippingStatuses", ShippingStatus.values());
        return "orders/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("form") OrderForm form, RedirectAttributes ra) {
        List<OrderItem> items = form.toItems();
        ensureDefaultPriceIfNull(items);              // << auto fill giá gốc nếu để trống
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
        // Lấy 200 khách đầu tiên để hiển thị dropdown; tuỳ nhu cầu bạn chỉnh size
        return customerRepo.findAll(PageRequest.of(0, 200, Sort.by("id").descending())).getContent();
    }

    // ===================== DTO FORM =====================
    @Data
    public static class OrderForm {
        private Long id;
        private Long customerId;
        private String code;
        private String shippingAddress;
        private String shippingProvider;
        private String trackingCode;
        private String noteInternal;
        private OrderStatus status = OrderStatus.PENDING;
        private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
        private ShippingStatus shippingStatus = ShippingStatus.NOT_SHIPPED;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal shippingFee = BigDecimal.ZERO;

        // items (parallel arrays)
        private List<Long> variantIds = new ArrayList<>();
        private List<BigDecimal> prices = new ArrayList<>();
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
            o.setStatus(status);
            o.setPaymentStatus(paymentStatus);
            o.setShippingStatus(shippingStatus);
            o.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
            o.setShippingFee(shippingFee != null ? shippingFee : BigDecimal.ZERO);
            return o;
        }

        public List<OrderItem> toItems() {
            List<OrderItem> list = new ArrayList<>();
            for (int i = 0; i < variantIds.size(); i++) {
                Long vid = variantIds.get(i);
                if (vid == null) continue;

                BigDecimal price = (i < prices.size() && prices.get(i) != null) ? prices.get(i) : null; // cho phép null để auto-fill
                Integer qty = (i < qtys.size() && qtys.get(i) != null) ? qtys.get(i) : 0;

                ProductVariant v = new ProductVariant();
                v.setId(vid);

                OrderItem it = new OrderItem();
                it.setVariant(v);
                it.setPrice(price); // có thể là null -> controller sẽ auto-fill
                it.setQty(qty);
                it.setTotal(price != null ? price.multiply(BigDecimal.valueOf(qty)) : BigDecimal.ZERO);
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
            f.setShippingStatus(o.getShippingStatus());
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
}
