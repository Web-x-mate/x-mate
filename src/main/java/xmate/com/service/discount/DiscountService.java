// src/main/java/xmate/com/service/discount/DiscountService.java
package xmate.com.service.discount;

import org.springframework.data.domain.*;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.discount.Discount;
import xmate.com.entity.discount.DiscountUsage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface DiscountService {

    // CRUD
    Page<Discount> search(String q, DiscountKind type, DiscountValueType valueType, Pageable pageable);
    Discount get(Long id);
    Discount create(Discount d);
    Discount update(Long id, Discount d);
    void delete(Long id);

    // Kiểm tra & chọn discount (CODE hoặc AUTO)
    Optional<Discount> pickApplicable(String couponCodeOrNull,
                                      Long customerIdOrNull,
                                      BigDecimal orderSubtotal,
                                      LocalDateTime now);

    // Ghi nhận usage sau khi order thành công
    DiscountUsage recordUsage(Long discountId, Long orderId, Long customerIdOrNull);
}
