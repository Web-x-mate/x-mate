package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import xmate.com.controller.client.support.ClientCatalogViewService;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.cart.CartItemDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.discount.DiscountRepository;
import xmate.com.security.SecurityUtils;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.PricingService;
import xmate.com.service.customer.AddressService;

import java.math.BigDecimal;
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
    private final ClientCatalogViewService catalogViewService;

    @GetMapping
    public String page(Model model, Authentication auth) {
        try {
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

            model.addAttribute("addresses", addressService.listForCurrentUser());

            CartDto currentCart = cartService.getCartForCurrentUser();
            String appliedCouponCode = currentCart.appliedCoupon();
            PricingDto pricing = pricingService.calculate(null, appliedCouponCode);

            List<Discount> availableCoupons = couponRepo
                    .findActiveByKinds(LocalDateTime.now(), List.of(DiscountKind.AUTO, DiscountKind.CODE))
                    .stream()
                    .filter(c -> {
                        BigDecimal min = c.getMinOrder() == null ? BigDecimal.ZERO : c.getMinOrder();
                        return BigDecimal.valueOf(pricing.subtotal()).compareTo(min) >= 0;
                    })
                    .toList();

            model.addAttribute("pricing", pricing);
            model.addAttribute("appliedCouponCode", appliedCouponCode);
            model.addAttribute("cartItems", currentCart.items());
            model.addAttribute("availableCoupons", availableCoupons);

            model.addAttribute("pageTitle", "X-Mate | Thanh toán");
            model.addAttribute("primaryCategories", catalogViewService.buildPrimaryNav());
            model.addAttribute("searchQuery", null);
            int cartQuantity = currentCart.items().stream().mapToInt(CartItemDto::qty).sum();
            model.addAttribute("cartQuantity", cartQuantity);
        } catch (IllegalStateException e) {
            return "redirect:/login";
        }
        return "checkout/index";
    }
}
