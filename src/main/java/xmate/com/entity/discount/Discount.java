package xmate.com.entity.discount;

import jakarta.persistence.*;
import lombok.*;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** CODE / AUTO ... */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountKind type = DiscountKind.CODE;

    @Column(unique = true, length = 60)
    private String code;

    /** PERCENT / FIXED */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false)
    private DiscountValueType valueType = DiscountValueType.PERCENT;

    /** Số % hoặc số tiền */
    @Column(name = "value_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal valueAmount;

    /** Đơn tối thiểu để áp dụng */
    @Column(name = "min_order", precision = 12, scale = 2)
    private BigDecimal minOrder;

    @Column(name = "conditions_json", columnDefinition = "json")
    private String conditionsJson;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** Tổng lượt cho phép */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Đã dùng bao nhiêu lượt */
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false, length = 10)
    private String status = "ACTIVE";

    /* ---------- Helpers tiện dùng (an toàn null) ---------- */

    /** Mức đơn tối thiểu để áp dụng (không trừ gì cả) */
    public BigDecimal getMinSubtotal() {
        return nz(minOrder);
    }

    /** Giá trị raw (tiền hoặc %) */
    public BigDecimal getValue() {
        return nz(valueAmount);
    }

    /** Số lượt đã dùng (int an toàn) */
    public int getUsed() {
        return usedCount == null ? 0 : usedCount;
    }

    /** Giới hạn giảm tối đa cho một lần áp (có thể tuỳ chỉnh theo nhu cầu) */
    public BigDecimal getMaxDiscount() {
        // Nếu không cần cap, trả về null; còn giữ cố định 500k như bạn đang dùng:
        return BigDecimal.valueOf(1_500_000);
    }

    /** Coupon còn hiệu lực tại thời điểm now? */
    public boolean isActiveAt(LocalDateTime now) {
        if (!"ACTIVE".equalsIgnoreCase(status)) return false;
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        if (usageLimit != null && getUsed() >= usageLimit) return false;
        return true;
    }

    /** Coupon còn hiệu lực "ngay bây giờ"? */
    public boolean isActiveNow() {
        return isActiveAt(LocalDateTime.now());
    }

    /**
     * Tính số tiền giảm cho một tổng tiền (BigDecimal).
     * Không áp điều kiện minSubtotal ở đây để tuỳ nơi gọi kiểm tra,
     * nếu muốn bạn có thể thêm điều kiện vào.
     */
    public BigDecimal calcDiscount(BigDecimal baseAmount) {
        BigDecimal base = nz(baseAmount);
        BigDecimal off;
        if (valueType == DiscountValueType.PERCENT) {
            off = base.multiply(getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            off = getValue();
        }
        BigDecimal cap = getMaxDiscount();
        if (cap != null && off.compareTo(cap) > 0) off = cap;
        if (off.compareTo(base) > 0) off = base;
        return off.max(BigDecimal.ZERO);
    }

    /* ---------- private ---------- */
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
