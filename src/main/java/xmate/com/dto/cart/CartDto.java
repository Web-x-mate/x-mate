// src/main/java/dto/cart/CartDto.java
package xmate.com.dto.cart;

import java.util.List;

public record CartDto(
        List<CartItemDto> items,
        long subtotal,
        long discount,
        long shipping,
        long total,
        String appliedCoupon
) {}
