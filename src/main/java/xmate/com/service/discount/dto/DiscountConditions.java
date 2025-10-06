package xmate.com.service.discount.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class DiscountConditions {
    private Scope scope = new Scope();
    private Integer minQtyPerItem = 1;     // mặc định 1
    private String includeMode = "ANY";    // ANY | ALL
    private BigDecimal capPerItem;         // trần giảm mỗi dòng
    private BigDecimal capPerOrder;        // trần giảm toàn đơn

    @Getter @Setter @NoArgsConstructor
    public static class Scope {
        private List<Long> categoryIds;
        private List<Long> productIds;
        private List<Long> variantIds;
    }
}
