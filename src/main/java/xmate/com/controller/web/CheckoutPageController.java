// src/main/java/xmate/com/controller/web/CheckoutPageController.java
package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.discount.Discount;
import xmate.com.entity.common.DiscountKind;
import xmate.com.repo.discount.DiscountRepository;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.PricingService;
import xmate.com.service.customer.AddressService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/checkout")
public class CheckoutPageController {

    private final AddressService addressService;
    private final CartService cartService;
    private final PricingService pricingService;
    private final DiscountRepository couponRepo;

    @GetMapping
    public String page(Model model) {
        try {
            // Địa chỉ của user hiện tại
            model.addAttribute("addresses", addressService.listForCurrentUser());

            // Lấy giỏ hiện tại
            CartDto currentCart = cartService.getCartForCurrentUser();
            if (currentCart.items() == null || currentCart.items().isEmpty()) {
                return "redirect:/cart";
            }

            // Coupon đang áp dụng (nếu có)
            String appliedCouponCode = currentCart.appliedCoupon();

            // Tính giá tiền hiện tại (giữ nguyên tham số theo service bạn đang dùng)
            PricingDto pricing = pricingService.calculate(null, appliedCouponCode);

            // Lấy danh sách coupon còn hiệu lực theo KIND
            List<Discount> availableCoupons =
                    couponRepo.findActiveByKind(LocalDateTime.now(), DiscountKind.AUTO);

            // Gắn model
            model.addAttribute("pricing", pricing);
            model.addAttribute("appliedCouponCode", appliedCouponCode);
            model.addAttribute("cartItems", currentCart.items());
            model.addAttribute("availableCoupons", availableCoupons);

        } catch (IllegalStateException e) {
            // Chưa đăng nhập
            return "redirect:/login";
        }
        return "checkout/index";
    }
}
