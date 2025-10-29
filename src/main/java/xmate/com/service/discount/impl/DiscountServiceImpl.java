package xmate.com.service.discount.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.discount.*;
import xmate.com.repo.discount.*;
import xmate.com.service.discount.DiscountService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountRepository discountRepo;
    private final DiscountUsageRepository usageRepo;

    @Override
    public Page<Discount> search(String q, DiscountKind type, DiscountValueType valueType, Pageable pageable) {
        String qq = (q == null) ? "" : q.trim();
        return discountRepo.search(qq, type, valueType, pageable);
    }

    @Override
    public Discount get(Long id) {
        return discountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Discount not found"));
    }

    @Override @Transactional
    public Discount create(Discount d) {
        d.setId(null);
        if (d.getUsedCount() == null) d.setUsedCount(0);
        return discountRepo.save(d);
    }

    @Override @Transactional
    public Discount update(Long id, Discount d) {
        Discount cur = get(id);
        cur.setType(d.getType());
        cur.setCode(d.getCode());
        cur.setValueType(d.getValueType());
        cur.setValueAmount(d.getValueAmount());
        cur.setMinOrder(d.getMinOrder());
        cur.setConditionsJson(d.getConditionsJson());
        cur.setStartAt(d.getStartAt());
        cur.setEndAt(d.getEndAt());
        cur.setUsageLimit(d.getUsageLimit());
        cur.setStatus(d.getStatus());
        return discountRepo.save(cur);
    }

    @Override @Transactional
    public void delete(Long id) {
        discountRepo.deleteById(id);
    }

    @Override
    public Optional<Discount> pickApplicable(String couponCodeOrNull,
                                             Long customerIdOrNull,
                                             BigDecimal orderSubtotal,
                                             LocalDateTime now) {
        if (orderSubtotal == null) orderSubtotal = BigDecimal.ZERO;
        if (now == null) now = LocalDateTime.now();

        if (couponCodeOrNull != null && !couponCodeOrNull.isBlank()) {
            Optional<Discount> opt = discountRepo.findByCodeIgnoreCase(couponCodeOrNull.trim());
            if (opt.isEmpty()) return Optional.empty();
            Discount d = opt.get();
            if (!isActive(d, now)) return Optional.empty();
            if (!passUsageLimit(d)) return Optional.empty();
            if (!passMinOrder(d, orderSubtotal)) return Optional.empty();
            return Optional.of(d);
        }

        // FIX: dùng hàm repo mới (danh sách kinds) cho AUTO
        List<Discount> autos = discountRepo.findActiveByKinds(now, List.of(DiscountKind.AUTO));
        for (Discount d : autos) {
            if (!passUsageLimit(d)) continue;
            if (!passMinOrder(d, orderSubtotal)) continue;
            return Optional.of(d);
        }
        return Optional.empty();
    }

    private boolean isActive(Discount d, LocalDateTime now) {
        if (!"ACTIVE".equalsIgnoreCase(d.getStatus())) return false;
        if (d.getStartAt() != null && d.getStartAt().isAfter(now)) return false;
        if (d.getEndAt() != null && d.getEndAt().isBefore(now)) return false;
        if (d.getUsageLimit() != null && d.getUsedCount() != null && d.getUsedCount() >= d.getUsageLimit()) return false;
        return true;
    }

    // FIX: kiểm tra limit theo usedCount tổng (đúng với cách bạn đang tăng usedCount)
    private boolean passUsageLimit(Discount d) {
        if (d.getUsageLimit() == null) return true;
        Integer used = d.getUsedCount() == null ? 0 : d.getUsedCount();
        return used < d.getUsageLimit();
    }

    private boolean passMinOrder(Discount d, BigDecimal subtotal) {
        BigDecimal min = d.getMinOrder() == null ? BigDecimal.ZERO : d.getMinOrder();
        return subtotal.compareTo(min) >= 0;
    }

    @Override @Transactional
    public DiscountUsage recordUsage(Long discountId, Long orderId, Long customerIdOrNull) {
        Discount discount = discountRepo.findById(discountId)
                .orElseThrow(() -> new IllegalArgumentException("Discount not found"));

        DiscountUsageId id = new DiscountUsageId(discountId, orderId);
        if (usageRepo.existsById(id)) {
            return usageRepo.findById(id).get();
        }

        DiscountUsage u = DiscountUsage.builder()
                .id(id)
                .discount(discount)
                .order(xmate.com.entity.sales.Order.builder().id(orderId).build())
                .customer(customerIdOrNull != null ? xmate.com.entity.customer.Customer.builder().id(customerIdOrNull).build() : null)
                .usedAt(LocalDateTime.now())
                .build();
        DiscountUsage saved = usageRepo.save(u);

        // tăng used_count
        discount.setUsedCount((discount.getUsedCount() == null ? 0 : discount.getUsedCount()) + 1);
        discountRepo.save(discount);

        return saved;
    }
}


// package xmate.com.service.discount.impl;

// import lombok.RequiredArgsConstructor;
// import org.springframework.data.domain.*;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import xmate.com.entity.common.DiscountKind;
// import xmate.com.entity.common.DiscountValueType;
// import xmate.com.entity.discount.*;
// import xmate.com.repo.discount.*;
// import xmate.com.service.discount.DiscountService;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.*;

// @Service
// @RequiredArgsConstructor
// public class DiscountServiceImpl implements DiscountService {

//     private final DiscountRepository discountRepo;
//     private final DiscountUsageRepository usageRepo;

//     @Override
//     public Page<Discount> search(String q, DiscountKind type, DiscountValueType valueType, Pageable pageable) {
//         String qq = (q == null) ? "" : q.trim();
//         return discountRepo.search(qq, type, valueType, pageable);
//     }

//     @Override
//     public Discount get(Long id) {
//         return discountRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Discount not found"));
//     }

//     @Override @Transactional
//     public Discount create(Discount d) {
//         d.setId(null);
//         if (d.getUsedCount() == null) d.setUsedCount(0);
//         return discountRepo.save(d);
//     }

//     @Override @Transactional
//     public Discount update(Long id, Discount d) {
//         Discount cur = get(id);
//         cur.setType(d.getType());
//         cur.setCode(d.getCode());
//         cur.setValueType(d.getValueType());
//         cur.setValueAmount(d.getValueAmount());
//         cur.setMinOrder(d.getMinOrder());
//         cur.setConditionsJson(d.getConditionsJson());
//         cur.setStartAt(d.getStartAt());
//         cur.setEndAt(d.getEndAt());
//         cur.setUsageLimit(d.getUsageLimit());
//         cur.setStatus(d.getStatus());
//         return discountRepo.save(cur);
//     }

//     @Override @Transactional
//     public void delete(Long id) {
//         discountRepo.deleteById(id);
//     }

//     @Override
//     public Optional<Discount> pickApplicable(String couponCodeOrNull,
//                                              Long customerIdOrNull,
//                                              BigDecimal orderSubtotal,
//                                              LocalDateTime now) {
//         if (orderSubtotal == null) orderSubtotal = BigDecimal.ZERO;
//         if (now == null) now = LocalDateTime.now();

//         // Có mã -> lấy đúng mã rồi tự kiểm
//         if (couponCodeOrNull != null && !couponCodeOrNull.isBlank()) {
//             Optional<Discount> opt = discountRepo.findByCodeIgnoreCase(couponCodeOrNull.trim());
//             if (opt.isEmpty()) return Optional.empty();
//             Discount d = opt.get();
//             if (!isActive(d, now)) return Optional.empty();
//             if (!passUsageLimit(d, customerIdOrNull)) return Optional.empty();
//             if (!passMinOrder(d, orderSubtotal)) return Optional.empty();
//             return Optional.of(d);
//         }

//         // AUTO -> lấy list active theo thời điểm & loại, rồi lọc thêm usage/min_order
//         List<Discount> autos = discountRepo.findActiveByKind(now, DiscountKind.AUTO);
//         for (Discount d : autos) {
//             if (!passUsageLimit(d, customerIdOrNull)) continue;
//             if (!passMinOrder(d, orderSubtotal)) continue;
//             // có thể return cái đầu tiên; hoặc để engine chọn "best" theo items
//             return Optional.of(d);
//         }
//         return Optional.empty();
//     }



//     private boolean isActive(Discount d, LocalDateTime now) {
//         if (!"ACTIVE".equalsIgnoreCase(d.getStatus())) return false;
//         if (d.getStartAt() != null && d.getStartAt().isAfter(now)) return false;
//         if (d.getEndAt() != null && d.getEndAt().isBefore(now)) return false;
//         if (d.getUsageLimit() != null && d.getUsedCount() != null && d.getUsedCount() >= d.getUsageLimit()) return false;
//         return true;
//     }

//     private boolean passUsageLimit(Discount d, Long customerId) {
//         if (d.getUsageLimit() == null) return true;
//         long used = usageRepo.countByDiscountId(d.getId());
//         return used < d.getUsageLimit();
//     }

//     private boolean passMinOrder(Discount d, BigDecimal subtotal) {
//         BigDecimal min = d.getMinOrder() == null ? BigDecimal.ZERO : d.getMinOrder();
//         return subtotal.compareTo(min) >= 0;
//     }

//     @Override @Transactional
//     public DiscountUsage recordUsage(Long discountId, Long orderId, Long customerIdOrNull) {
//         Discount discount = discountRepo.findById(discountId)
//                 .orElseThrow(() -> new IllegalArgumentException("Discount not found"));

//         DiscountUsageId id = new DiscountUsageId(discountId, orderId);
//         if (usageRepo.existsById(id)) {
//             return usageRepo.findById(id).get();
//         }

//         DiscountUsage u = DiscountUsage.builder()
//                 .id(id)
//                 .discount(discount)
//                 .order(xmate.com.entity.sales.Order.builder().id(orderId).build())
//                 .customer(customerIdOrNull != null ? xmate.com.entity.customer.Customer.builder().id(customerIdOrNull).build() : null)
//                 .usedAt(LocalDateTime.now())
//                 .build();
//         DiscountUsage saved = usageRepo.save(u);

//         // tăng used_count
//         discount.setUsedCount((discount.getUsedCount() == null ? 0 : discount.getUsedCount()) + 1);
//         discountRepo.save(discount);

//         return saved;
//     }
// }
