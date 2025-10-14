package xmate.com.service.cart.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.cart.CartItemDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.cart.Cart;
import xmate.com.entity.cart.CartItem;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.cart.CartItemRepository;
import xmate.com.repo.cart.CartRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.PricingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductVariantRepository variantRepo;
    private final CustomerRepository userRepo;
    private final PricingService pricingService;

    @Override
    public CartDto getCartForCurrentUser() {
        Long uid = currentUserId();
        Cart cart = ensureCart(uid);
        CartDto base = toDtoItemsOnly(cart);

        String appliedCode = cart.getAppliedCouponCode();
        PricingDto pricing = pricingService.calculate(null, appliedCode);
        return mergeCartWithPricing(base, pricing, appliedCode);
    }

    @Override
    public CartDto addItem(Long variantId, Integer qty) {
        if (qty == null || qty <= 0) qty = 1;

        Long uid = currentUserId();
        Cart cart = ensureCart(uid);
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm: " + variantId));

        CartItem item = cartItemRepo.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setCart(cart);
                    ci.setVariant(variant);
                    ci.setQty(0);
                    return ci;
                });

        int newQty = item.getQty() + qty;

        // Nếu stock là primitive int (không null), chỉ cần giới hạn theo stock
        // Nếu stock là Integer (có thể null), logic dưới vẫn ổn
        Integer stock = null;
        try {
            stock = (Integer) ProductVariant.class.getMethod("getStock").invoke(variant);
        } catch (Exception ignore) {}
        if (stock != null && newQty > stock) {
            newQty = stock;
        }

        item.setQty(newQty);

        // Lưu snapshot giá (long). Nếu Variant.price là BigDecimal, convert -> long
        long variantPrice = priceToLong(variant.getPrice());
        item.setPriceSnap(Optional.ofNullable(item.getPriceSnap()).orElse(variantPrice));

        cartItemRepo.save(item);
        return getCartForCurrentUser();
    }

    @Override
    public CartDto updateQty(Long itemId, Integer qty) {
        if (qty == null || qty <= 0) qty = 1;

        CartItem item = findItemInCurrentUserCart(itemId);
        ProductVariant pv = item.getVariant();

        Integer stock = null;
        try {
            stock = (Integer) ProductVariant.class.getMethod("getStock").invoke(pv);
        } catch (Exception ignore) {}
        if (stock != null && qty > stock) {
            qty = stock;
        }

        item.setQty(qty);
        cartItemRepo.save(item);

        return getCartForCurrentUser();
    }

    @Override
    public CartDto removeItem(Long itemId) {
        CartItem item = findItemInCurrentUserCart(itemId);
        cartItemRepo.delete(item);
        return getCartForCurrentUser();
    }

    @Override
    public CartDto applyCoupon(String code) {
        Long uid = currentUserId();
        Cart cart = ensureCart(uid);

        String normalized = (code == null || code.isBlank()) ? null : code.trim();
        PricingDto pricing = pricingService.calculate(null, normalized);

        cart.setAppliedCouponCode(pricing.discount() > 0 ? normalized : null);
        cartRepo.save(cart);

        return getCartForCurrentUser();
    }

    @Override
    public void clearCartForCurrentUser() {
        Long uid = currentUserId();
        Cart cart = ensureCart(uid);
        cartItemRepo.deleteByCartId(cart.getId());
    }

    // ===== Helpers =====

    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = xmate.com.security.SecurityUtils.resolveEmail(auth);
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Bạn chưa đăng nhập");
        }
        return userRepo.findByEmailIgnoreCase(email)
                .map(Customer::getId)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy user: " + email));
    }


    private Cart ensureCart(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(userRepo.findById(userId).orElseThrow());
            return cartRepo.save(c);
        });
    }

    private CartItem findItemInCurrentUserCart(Long itemId) {
        Long uid = currentUserId();
        Cart cart = ensureCart(uid);
        return cartItemRepo.findById(itemId)
                .filter(ci -> ci.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng."));
    }

    private CartDto toDtoItemsOnly(Cart cart) {
        List<CartItem> items = cartItemRepo.findByCartIdWithDetails(cart.getId());

        List<CartItemDto> itemDtos = items.stream().map(ci -> {
            // priceSnap (long) ưu tiên; nếu null, lấy Variant.price (BigDecimal → long)
            long price = Optional.ofNullable(ci.getPriceSnap())
                    .orElseGet(() -> priceToLong(ci.getVariant().getPrice()));

            String image = null;
            try {
                // cố gắng lấy variant.getImage() trước, nếu null lấy product.getImage()
                Object vImg = ci.getVariant().getClass().getMethod("getImage").invoke(ci.getVariant());
                Object pImg = ci.getVariant().getProduct().getClass().getMethod("getImage").invoke(ci.getVariant().getProduct());
                image = vImg != null ? String.valueOf(vImg) : (pImg != null ? String.valueOf(pImg) : null);
            } catch (Exception ignore) {}

            return new CartItemDto(
                    ci.getId(),
                    ci.getVariant().getId(),
                    ci.getVariant().getProduct().getName(),
                    ci.getVariant().getSku(),
                    ci.getVariant().getSize(),
                    ci.getVariant().getColor(),
                    price,
                    ci.getQty(),
                    price * ci.getQty(),
                    image
            );
        }).collect(Collectors.toList());

        long subtotal = itemDtos.stream().mapToLong(CartItemDto::lineTotal).sum();
        return new CartDto(itemDtos, subtotal, 0L, 0L, subtotal, cart.getAppliedCouponCode());
    }

    private CartDto mergeCartWithPricing(CartDto base, PricingDto pricing, String appliedCode) {
        return new CartDto(
                base.items(),
                pricing.subtotal(),
                pricing.discount(),
                pricing.shipping(),
                pricing.total(),
                (pricing.discount() > 0 ? appliedCode : base.appliedCoupon())
        );
    }

    private long priceToLong(Object priceObj) {
        if (priceObj == null) return 0L;
        if (priceObj instanceof Long l) return l;
        if (priceObj instanceof Integer i) return i.longValue();
        if (priceObj instanceof BigDecimal bd) return bd.longValue();
        if (priceObj instanceof String s) {
            try { return new BigDecimal(s).longValue(); } catch (Exception ignored) {}
        }
        return 0L;
    }
}
