package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.cart.AddItemReq;
import xmate.com.dto.cart.ApplyCouponReq;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.cart.UpdateQtyReq;
import xmate.com.service.cart.CartService;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartApiController {

    private final CartService cartService;

    /** Lấy thông tin giỏ hàng hiện tại */
    @GetMapping
    public CartDto getCart() {
        return cartService.getCartForCurrentUser();
    }

    /** Thêm sản phẩm vào giỏ */
    @PostMapping
    public CartDto addItem(@RequestBody AddItemReq req) {
        return cartService.addItem(req.variantId(), req.qty());
    }

    /** Cập nhật số lượng sản phẩm */
    @PostMapping("/qty")
    public CartDto updateQty(@RequestBody UpdateQtyReq req) {
        return cartService.updateQty(req.itemId(), req.qty());
    }

    /** Xóa sản phẩm khỏi giỏ */
    @DeleteMapping("/items/{itemId}")
    public CartDto removeItem(@PathVariable Long itemId) {
        return cartService.removeItem(itemId);
    }

    /** Áp dụng mã giảm giá */
    @PostMapping("/coupon")
    public CartDto applyCoupon(@RequestBody ApplyCouponReq req) {
        return cartService.applyCoupon(req.code());
    }
}