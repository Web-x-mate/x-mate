// src/main/java/xmate/com/service/discount/DiscountEngine.java
package xmate.com.service.discount;

import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.entity.discount.Discount;

import java.math.BigDecimal;
import java.util.List;

public interface DiscountEngine {
    AppliedDiscount applyOnItems(Discount discount, Order draftOrder, List<OrderItem> items);
    class AppliedDiscount {
        public Long discountId;
        public BigDecimal totalDiscount = BigDecimal.ZERO;
        public java.util.Map<Integer, BigDecimal> perItem = new java.util.HashMap<>(); // key = index item
    }
}
