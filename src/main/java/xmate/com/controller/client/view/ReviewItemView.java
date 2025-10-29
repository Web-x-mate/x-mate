package xmate.com.controller.client.view;

public record ReviewItemView(
        Long orderItemId,
        String orderCode,
        String orderDateText,
        Long productId,
        Long variantId,
        String productName,
        String slug,
        String thumbnail,
        String color,
        String size,
        Integer existingRating,
        String existingContent,
        String reviewStatusLabel,
        String reviewStatusClass,
        boolean canReview
) {}

