package xmate.com.service.cart;

import xmate.com.dto.cart.CartDto;

public interface CartService {

    /** Lấy thông tin giỏ hàng chi tiết của người dùng đang đăng nhập. */
    CartDto getCartForCurrentUser();

    /** Thêm một sản phẩm vào giỏ hàng. */
    CartDto addItem(Long variantId, Integer qty);

    /** Cập nhật số lượng của một sản phẩm trong giỏ hàng. */
    CartDto updateQty(Long itemId, Integer qty);

    /** Xóa một sản phẩm khỏi giỏ hàng. */
    CartDto removeItem(Long itemId);

    /** Áp dụng một mã giảm giá vào giỏ hàng. */
    CartDto applyCoupon(String code);

    /** Xóa tất cả sản phẩm trong giỏ hàng của người dùng hiện tại. */
    void clearCartForCurrentUser();
}