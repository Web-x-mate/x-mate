package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.discount.DiscountRepository;
import xmate.com.security.SecurityUtils;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.PricingService;
import xmate.com.service.customer.AddressService;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal; // FIX: cần import
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
    private final CustomerRepository customerRepo;

    @GetMapping
    public String page(Model model, Authentication auth) {
        try {
            // Prefill from current customer
            String email = SecurityUtils.resolveEmail(auth);
            model.addAttribute("authedEmail", email);
            if (email != null && !email.isBlank()) {
                customerRepo.findByEmailIgnoreCase(email).ifPresent(c -> {
                    model.addAttribute("meEmail", c.getEmail());
                    model.addAttribute("mePhone", c.getPhone());
                    model.addAttribute("meFullname", c.getFullname());
                    model.addAttribute("meGender", c.getGender());
                });
            }

            // Addresses for selection
            model.addAttribute("addresses", addressService.listForCurrentUser());

            // Cart + pricing
            CartDto currentCart = cartService.getCartForCurrentUser();
            String appliedCouponCode = currentCart.appliedCoupon();
            PricingDto pricing = pricingService.calculate(null, appliedCouponCode);

            // FIX: lấy cả AUTO + CODE đang active
            List<Discount> availableCoupons =
        couponRepo.findActiveByKinds(LocalDateTime.now(), List.of(DiscountKind.AUTO, DiscountKind.CODE))
                .stream()
                .filter(c -> {
                    BigDecimal min = (c.getMinOrder() == null ? BigDecimal.ZERO : c.getMinOrder());
                    return BigDecimal.valueOf(pricing.subtotal()).compareTo(min) >= 0;
                })
                .toList();
model.addAttribute("availableCoupons", availableCoupons);

            model.addAttribute("pricing", pricing);
            model.addAttribute("appliedCouponCode", appliedCouponCode);
            model.addAttribute("cartItems", currentCart.items());
            model.addAttribute("availableCoupons", availableCoupons);

        } catch (IllegalStateException e) {
            return "redirect:/login";
        }
        return "checkout/index";
    }
}

// package xmate.com.controller.web;

// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Controller;
// import org.springframework.ui.Model;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import xmate.com.dto.cart.CartDto;
// import xmate.com.dto.checkout.PricingDto;
// import xmate.com.entity.common.DiscountKind;
// import xmate.com.entity.discount.Discount;
// import xmate.com.repo.customer.CustomerRepository;
// import xmate.com.repo.discount.DiscountRepository;
// import xmate.com.security.SecurityUtils;
// import xmate.com.service.cart.CartService;
// import xmate.com.service.cart.PricingService;
// import xmate.com.service.customer.AddressService;
// import org.springframework.security.core.Authentication;

// import java.time.LocalDateTime;
// import java.util.List;

// @Controller
// @RequiredArgsConstructor
// @RequestMapping("/checkout")
// public class CheckoutPageController {

//     private final AddressService addressService;
//     private final CartService cartService;
//     private final PricingService pricingService;
//     private final DiscountRepository couponRepo;
//     private final CustomerRepository customerRepo;

//     @GetMapping
//     public String page(Model model, Authentication auth) {
//         try {
//             // Prefill from current customer
//             String email = SecurityUtils.resolveEmail(auth);
//             model.addAttribute("authedEmail", email);
//             if (email != null && !email.isBlank()) {
//                 customerRepo.findByEmailIgnoreCase(email).ifPresent(c -> {
//                     model.addAttribute("meEmail", c.getEmail());
//                     model.addAttribute("mePhone", c.getPhone());
//                     model.addAttribute("meFullname", c.getFullname());
//                     model.addAttribute("meGender", c.getGender());
//                 });
//             }

//             // Addresses for selection
//             model.addAttribute("addresses", addressService.listForCurrentUser());

//             // Cart + pricing
//             CartDto currentCart = cartService.getCartForCurrentUser();
//             String appliedCouponCode = currentCart.appliedCoupon();
//             PricingDto pricing = pricingService.calculate(null, appliedCouponCode);
//             List<Discount> availableCoupons =
//                     couponRepo.findActiveByKind(LocalDateTime.now(), DiscountKind.)
//                             .stream()
//                             .filter(c -> pricing.getSubtotal().compareTo(
//                                     c.getMinSubtotal() != null ? c.getMinSubtotal() : BigDecimal.ZERO) >= 0)
//                             .toList();

//             model.addAttribute("pricing", pricing);
//             model.addAttribute("appliedCouponCode", appliedCouponCode);
//             model.addAttribute("cartItems", currentCart.items());
//             model.addAttribute("availableCoupons", availableCoupons);

//         } catch (IllegalStateException e) {
//             return "redirect:/login";
//         }
//         return "checkout/index";
//     }
// }
