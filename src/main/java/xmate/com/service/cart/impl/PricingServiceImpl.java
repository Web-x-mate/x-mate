// src/main/java/xmate/com/service/cart/impl/PricingServiceImpl.java
package xmate.com.service.cart.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.cart.CartItemRepository;
import xmate.com.repo.discount.DiscountRepository;
import xmate.com.repo.customer.CustomerRepository;   // <- đổi repo
import xmate.com.service.cart.PricingService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final CartItemRepository cartItemRepo;
    private final DiscountRepository couponRepo;
    private final CustomerRepository customerRepo;   // <- đổi tên field

    @Override
    public PricingDto calculate(Long addressId, String couponCode) {
        Long userId = currentUserId();
        long subtotalLong = Optional.ofNullable(cartItemRepo.sumSubtotalByUserId(userId)).orElse(0L);
        BigDecimal subtotal = BigDecimal.valueOf(subtotalLong);

        BigDecimal discount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            couponCode = couponCode.trim();
            Optional<Discount> oc = couponRepo.findByCodeIgnoreCase(couponCode);
            if (oc.isPresent() && subtotal.signum() > 0) {
                Discount c = oc.get();
//                if (isDiscountActiveNow(c) && passMinSubtotal(c, subtotal) && passUsageLimit(c)) {
//                    discount = calcDiscountAmount(c, subtotal);
//                }
            }
        }

        BigDecimal shipping = (subtotal.compareTo(BigDecimal.valueOf(500_000)) >= 0)
                ? BigDecimal.ZERO : BigDecimal.valueOf(30_000);
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        if (total.signum() < 0) total = BigDecimal.ZERO;

        return new PricingDto(subtotal.longValue(), discount.longValue(), shipping.longValue(), total.longValue());
    }

    // ------- helpers (giữ nguyên các hàm isDiscountActiveNow/pass*/calcDiscountAmount) -------

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = xmate.com.security.SecurityUtils.resolveEmail(auth);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Bạn chưa đăng nhập");
        }
        // Tìm theo email khách hàng; nếu chưa có thì auto-provision (tạo bản ghi)
        return customerRepo.findByEmailIgnoreCase(email)
                .map(Customer::getId)
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setEmail(email.trim().toLowerCase());
                    c.setEnabled(true);
                    return customerRepo.save(c).getId();
                });
    }
}
