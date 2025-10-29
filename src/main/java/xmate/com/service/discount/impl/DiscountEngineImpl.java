// src/main/java/xmate/com/service/discount/impl/DiscountEngineImpl.java
package xmate.com.service.discount.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.discount.Discount;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.service.discount.DiscountEngine;
import xmate.com.service.discount.dto.DiscountConditions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountEngineImpl implements DiscountEngine {

    private final ProductVariantRepository variantRepo;
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public AppliedDiscount applyOnItems(Discount discount, Order draftOrder, List<OrderItem> items) {
        AppliedDiscount rs = new AppliedDiscount();
        rs.discountId = discount.getId();
        if (items == null || items.isEmpty()) return rs;

        DiscountConditions cond = parse(discount.getConditionsJson());

        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            if (it.getVariant() == null || it.getVariant().getId() == null) continue;

            ProductVariant v = variantRepo.findById(it.getVariant().getId()).orElse(null);
            if (v == null) continue;

            Long productId  = v.getProduct() != null ? v.getProduct().getId() : null;
            Long categoryId = (v.getProduct() != null && v.getProduct().getCategory() != null)
                    ? v.getProduct().getCategory().getId() : null;

            if (!matchScope(cond, categoryId, productId, v.getId())) continue;

            // minQty có thể là Integer (nullable) trong điều kiện
            int minQty = cond.getMinQtyPerItem() == null ? 1 : cond.getMinQtyPerItem();
            // qty từ OrderItem là int (primitive) → không check null
            int qty = it.getQty();
            if (qty < minQty) continue;

            // price trong OrderItem là long (VND) → đưa sang BigDecimal để tính toán
            BigDecimal lineTotal = BigDecimal.valueOf(it.getPrice()).multiply(BigDecimal.valueOf(qty));
            BigDecimal lineDiscount;

            if (discount.getValueType() == DiscountValueType.PERCENT) {
                // valueAmount là BigDecimal, ví dụ 10 -> 10%
                lineDiscount = lineTotal.multiply(discount.getValueAmount())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else {
                // FIXED: số tiền giảm cho mỗi item
                lineDiscount = discount.getValueAmount().multiply(BigDecimal.valueOf(qty));
            }

            // Trần trên mỗi item (nếu có)
            if (cond.getCapPerItem() != null && lineDiscount.compareTo(cond.getCapPerItem()) > 0) {
                lineDiscount = cond.getCapPerItem();
            }
            // Không vượt quá line total
            if (lineDiscount.compareTo(lineTotal) > 0) lineDiscount = lineTotal;

            if (lineDiscount.signum() > 0) {
                rs.perItem.put(i, lineDiscount);
                total = total.add(lineDiscount);
            }
        }

        // Trần theo đơn (nếu có)
        if (cond.getCapPerOrder() != null && total.compareTo(cond.getCapPerOrder()) > 0) {
            total = cond.getCapPerOrder();
            // Nếu cần, có thể phân bổ lại theo tỷ lệ ở đây
        }

        rs.totalDiscount = total;
        return rs;
    }

    private DiscountConditions parse(String json) {
        try {
            return (json == null || json.isBlank())
                    ? new DiscountConditions()
                    : om.readValue(json, DiscountConditions.class);
        } catch (Exception e) {
            return new DiscountConditions();
        }
    }

    private boolean matchScope(DiscountConditions cond, Long catId, Long prodId, Long varId) {
        var s = cond.getScope();
        if (s == null) return true;

        boolean byCat = s.getCategoryIds() != null && !s.getCategoryIds().isEmpty()
                && catId != null && s.getCategoryIds().contains(catId);
        boolean byPro = s.getProductIds() != null && !s.getProductIds().isEmpty()
                && prodId != null && s.getProductIds().contains(prodId);
        boolean byVar = s.getVariantIds() != null && !s.getVariantIds().isEmpty()
                && varId != null && s.getVariantIds().contains(varId);

        boolean hasAny = (s.getCategoryIds() != null && !s.getCategoryIds().isEmpty())
                || (s.getProductIds() != null && !s.getProductIds().isEmpty())
                || (s.getVariantIds() != null && !s.getVariantIds().isEmpty());
        if (!hasAny) return true;

        if ("ALL".equalsIgnoreCase(cond.getIncludeMode())) {
            boolean ok = true;
            if (s.getCategoryIds() != null && !s.getCategoryIds().isEmpty()) ok &= byCat;
            if (s.getProductIds() != null && !s.getProductIds().isEmpty()) ok &= byPro;
            if (s.getVariantIds() != null && !s.getVariantIds().isEmpty()) ok &= byVar;
            return ok;
        }
        // ANY
        return byCat || byPro || byVar;
    }
}
