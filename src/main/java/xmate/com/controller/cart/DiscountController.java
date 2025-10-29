package xmate.com.controller.cart;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.discount.DiscountRepository;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class DiscountController {
    private final DiscountRepository discountRepo;
    private final ObjectMapper om = new ObjectMapper();

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") long subtotal,
            @AuthenticationPrincipal xmate.com.entity.customer.Customer me,
            @RequestParam(required = false) Long customerId
    ) {
        final long custId = (customerId != null ? customerId : (me != null ? me.getId() : 0L));
        final var nowVN = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));

        // LẤY THEO TYPE rồi tự lọc active trong Java (tránh lệch timezone)
        List<Discount> discounts = discountRepo.findActiveByKind(nowVN, DiscountKind.CODE).stream()
                .filter(d -> d.getMinSubtotal().longValue() <= subtotal)
                .filter(d -> isAllowedForCustomer(d.getConditionsJson(), custId))
                .toList();

        var nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        var d8 = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return discounts.stream().map(d -> {
            Map<String,Object> m = new java.util.LinkedHashMap<>();
            m.put("code", d.getCode());
            String title = switch (d.getValueType()) {
                case FIXED   -> "Giảm " + nf.format(d.getValue().longValue()) + "đ";
                case PERCENT -> "Giảm " + d.getValue().intValue() + "%";
            };
            m.put("title", title);
            m.put("minSubtotal", d.getMinSubtotal().longValue());
            m.put("description", "Đơn từ " + nf.format(d.getMinSubtotal().longValue()) + "đ");
            m.put("human", title + " • Đơn từ " + nf.format(d.getMinSubtotal().longValue()) + "đ");
            m.put("expiry", d.getEndAt() == null ? null : d8.format(d.getEndAt()));
            Integer remaining = null;
            if (d.getUsageLimit() != null && d.getUsageLimit() > 0) {
                int used = (d.getUsedCount() == null ? 0 : d.getUsedCount());
                remaining = Math.max(0, d.getUsageLimit() - used);
            }
            m.put("remaining", remaining);
            Long cap = readJsonLong(d.getConditionsJson(), "cap");
            if (cap != null) m.put("cap", cap);
            return m;
        }).toList();
    }

    private boolean isAllowedForCustomer(String json, long customerId) {
        if (customerId <= 0) return true;
        if (json == null || json.isBlank()) return true;
        try {
            var root = om.readTree(json);
            var allowed = root.get("allowed_customer_ids");
            if (allowed == null || !allowed.isArray()) return true;
            for (var n : allowed) if (n.asLong() == customerId) return true;
            return false;
        } catch (Exception e) { return true; }
    }
    private Long readJsonLong(String json, String key) {
        try {
            if (json == null || json.isBlank()) return null;
            var root = om.readTree(json);
            var v = root.get(key);
            return (v != null && v.isNumber()) ? v.asLong() : null;
        } catch (Exception ignored) { return null; }
    }
}


