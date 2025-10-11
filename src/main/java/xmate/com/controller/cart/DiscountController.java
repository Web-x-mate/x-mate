// src/main/java/xmate/com/controller/cart/DiscountController.java
package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.discount.DiscountRepository;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountRepository couponRepo;

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(defaultValue = "0") long subtotal) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        DateTimeFormatter d8 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Chỉ lấy các coupon đang ACTIVE, còn thời gian, chưa vượt usage limit
        List<Discount> coupons = couponRepo.findActiveByKind(LocalDateTime.now(), DiscountKind.AUTO);

        return coupons.stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("code", c.getCode());

                    // Tiêu đề: Giảm X đ (FIXED) hoặc Giảm Y%
                    String title = (c.getValueType() == DiscountValueType.FIXED)
                            ? "Giảm " + nf.format(nz(c.getValue()).longValue()) + "đ"
                            : "Giảm " + nz(c.getValue()).intValue() + "%";
                    m.put("title", title);

                    long min = nz(c.getMinSubtotal()).longValue();
                    String desc = "Đơn từ " + nf.format(min) + "đ";
                    m.put("description", desc);
                    m.put("human", title + " • " + desc);

                    m.put("expiry", c.getEndAt() == null ? null : d8.format(c.getEndAt()));

                    Integer remaining = null;
                    if (c.getUsageLimit() != null) {
                        int used = c.getUsed() ;
                        remaining = Math.max(0, c.getUsageLimit() - used);
                    }
                    m.put("remaining", remaining);

                    return m;
                })
                .collect(Collectors.toList());
    }

    private static java.math.BigDecimal nz(java.math.BigDecimal v) {
        return v == null ? java.math.BigDecimal.ZERO : v;
    }
}
