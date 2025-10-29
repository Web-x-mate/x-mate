package xmate.com.dto.cart;
public record CartItemDto(
        Long itemId,            // cart_item_id
        Long variantId,     // product_variant id
        String productName, // tên sản phẩm
        String sku,
        String size,
        String color,
        long price,         // đơn giá (snap)
        int qty,
        long lineTotal,
        String imageUrl
) {}
