package xmate.com.service.cart.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.cart.CartItemDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.cart.Cart;
import xmate.com.entity.cart.CartItem;
import xmate.com.entity.catalog.ProductMedia;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.cart.CartItemRepository;
import xmate.com.repo.cart.CartRepository;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.PricingService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String PLACEHOLDER_IMAGE = "/client/images/product-placeholder.svg";
    private static final long FREE_SHIP_MIN = 500_000L;
    private static final long DEFAULT_SHIPPING_FEE = 30_000L;
    private static final String CART_COOKIE_NAME = "cart_id";
    private static final int CART_COOKIE_MAX_AGE_DAYS = 30;
    private static final String GUEST_KEY_REQUEST_ATTR = CartServiceImpl.class.getName() + ".GUEST_KEY";

    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductVariantRepository variantRepo;
    private final ProductMediaRepository mediaRepo;
    private final CustomerRepository userRepo;
    private final PricingService pricingService;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Override
    public CartDto getCartForCurrentUser() {
        Cart cart = resolveCart();
        CartDto base = toDtoItemsOnly(cart);

        String appliedCode = cart.getAppliedCouponCode();
        PricingDto pricing = safePricing(base, appliedCode);
        return mergeCartWithPricing(base, pricing, appliedCode);
    }

    @Override
    public CartDto addItem(Long variantId, Integer qty) {
        if (qty == null || qty <= 0) qty = 1;

        Cart cart = resolveCart();
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

        CartItem item = findItemInCurrentCart(itemId);
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
        CartItem item = findItemInCurrentCart(itemId);
        cartItemRepo.delete(item);
        return getCartForCurrentUser();
    }

    @Override
    public CartDto applyCoupon(String code) {
        Cart cart = resolveCart();

        String normalized = (code == null || code.isBlank()) ? null : code.trim();
        CartDto base = toDtoItemsOnly(cart);
        PricingDto pricing = safePricing(base, normalized);

        cart.setAppliedCouponCode(pricing.discount() > 0 ? normalized : null);
        cartRepo.save(cart);

        return getCartForCurrentUser();
    }

    @Override
    public void clearCartForCurrentUser() {
        Cart cart = resolveCart();
        cartItemRepo.deleteByCartId(cart.getId());
        cart.setAppliedCouponCode(null);
        cartRepo.save(cart);
    }

    // ===== Helpers =====

    private Cart resolveCart() {
        Long userId = resolveCurrentUserId();
        if (userId != null) {
            return ensureUserCart(userId);
        }
        String guestKey = resolveGuestKey();
        if (!StringUtils.hasText(guestKey)) {
            throw new IllegalStateException("Unable to resolve cart.");
        }
        return ensureGuestCart(guestKey);
    }

    private Long resolveCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String email = xmate.com.security.SecurityUtils.resolveEmail(auth);
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userRepo.findByEmailIgnoreCase(email)
                .map(Customer::getId)
                .orElse(null);
    }

    private Cart ensureUserCart(Long userId) {
        return cartRepo.findByUserId(userId).orElseGet(() -> {
            Cart c = new Cart();
            c.setUser(userRepo.findById(userId).orElseThrow());
            c.setGuestKey(null);
            return cartRepo.save(c);
        });
    }

    private Cart ensureGuestCart(String guestKey) {
        return cartRepo.findByGuestKey(guestKey).orElseGet(() -> {
            Cart c = new Cart();
            c.setGuestKey(guestKey);
            return cartRepo.save(c);
        });
    }

    private CartItem findItemInCurrentCart(Long itemId) {
        Cart cart = resolveCart();
        return cartItemRepo.findById(itemId)
                .filter(ci -> ci.getCart().getId().equals(cart.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Item not found in cart."));
    }

    private String resolveGuestKey() {
        String cached = readGuestKeyFromRequest();
        if (StringUtils.hasText(cached)) {
            return cached;
        }
        String fromCookie = readGuestKeyFromCookie();
        if (StringUtils.hasText(fromCookie)) {
            storeGuestKeyInRequest(fromCookie);
            writeGuestCookie(fromCookie);
            return fromCookie;
        }
        String generated = UUID.randomUUID().toString();
        storeGuestKeyInRequest(generated);
        writeGuestCookie(generated);
        return generated;
    }

    private String readGuestKeyFromRequest() {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute(GUEST_KEY_REQUEST_ATTR);
        return attr instanceof String ? (String) attr : null;
    }

    private void storeGuestKeyInRequest(String guestKey) {
        if (request != null && StringUtils.hasText(guestKey)) {
            request.setAttribute(GUEST_KEY_REQUEST_ATTR, guestKey);
        }
    }

    private String readGuestKeyFromCookie() {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> CART_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private void writeGuestCookie(String guestKey) {
        if (response == null || !StringUtils.hasText(guestKey)) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(CART_COOKIE_NAME, guestKey)
                .httpOnly(false)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(CART_COOKIE_MAX_AGE_DAYS))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private CartDto toDtoItemsOnly(Cart cart) {
        List<CartItem> items = cartItemRepo.findByCartIdWithDetails(cart.getId());

        Map<Long, String> variantThumbCache = new HashMap<>();
        Map<Long, String> productThumbCache = new HashMap<>();

        List<CartItemDto> itemDtos = items.stream().map(ci -> {
            // priceSnap (long) ưu tiên; nếu null, lấy Variant.price (BigDecimal → long)
            long price = Optional.ofNullable(ci.getPriceSnap())
                    .orElseGet(() -> priceToLong(ci.getVariant().getPrice()));

            String image = resolveThumbnail(ci.getVariant(), variantThumbCache, productThumbCache);

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

    private PricingDto safePricing(CartDto base, String couponCode) {
        try {
            return pricingService.calculate(null, couponCode);
        } catch (IllegalStateException ex) {
            long subtotal = base.subtotal();
            long discount = 0L;
            long shipping = subtotal >= FREE_SHIP_MIN ? 0L : DEFAULT_SHIPPING_FEE;
            long total = Math.max(0L, subtotal - discount + shipping);
            return new PricingDto(subtotal, discount, shipping, total);
        }
    }

    private String resolveThumbnail(ProductVariant variant,
                                    Map<Long, String> variantThumbCache,
                                    Map<Long, String> productThumbCache) {
        if (variant == null) {
            return PLACEHOLDER_IMAGE;
        }

        Long variantId = variant.getId();
        if (variantId != null) {
            String cached = variantThumbCache.get(variantId);
            if (cached != null) {
                return cached;
            }
            String resolved = loadFirstMediaUrl(() ->
                    mediaRepo.findAllByVariant_IdOrderBySortOrderAsc(variantId)
            );
            if (resolved != null) {
                variantThumbCache.put(variantId, resolved);
                return resolved;
            }
        }

        if (variant.getProduct() != null && variant.getProduct().getId() != null) {
            Long productId = variant.getProduct().getId();
            String cached = productThumbCache.get(productId);
            if (cached != null) {
                return cached;
            }
            String resolved = loadFirstMediaUrl(() ->
                    mediaRepo.findAllByProduct_IdOrderBySortOrderAsc(productId)
            );
            if (resolved != null) {
                productThumbCache.put(productId, resolved);
                return resolved;
            }
        }

        return PLACEHOLDER_IMAGE;
    }

    private String loadFirstMediaUrl(Supplier<List<ProductMedia>> supplier) {
        try {
            return supplier.get().stream()
                    .filter(Objects::nonNull)
                    .map(ProductMedia::getUrl)
                    .filter(url -> url != null && !url.isBlank())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            return null;
        }
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
