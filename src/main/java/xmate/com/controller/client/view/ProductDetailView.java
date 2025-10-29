package xmate.com.controller.client.view;

import java.util.List;

public record ProductDetailView(
        Long id,
        String slug,
        String title,
        String description,
        String categoryName,
        String categorySlug,
        String finalPriceText,
        String originalPriceText,
        boolean hasDiscount,
        int discountPercent,
        String thumbnail,
        List<String> images,
        List<ProductColorView> colors,
        ProductColorView defaultColor,
        List<String> sizes,
        String defaultSize,
        boolean freeShip,
        List<String> vouchers,
        double priceForCart,
        String shareUrl
) {
}
