package xmate.com.controller.client.view;

import java.util.List;

public record OrderHistoryView(
        String code,
        String shortCode,
        String statusClass,
        String statusLabel,
        String paymentLabel,
        String createdAtText,
        int quantityTotal,
        List<Item> items,
        Shipping shipping,
        Totals totals,
        boolean hasDiscount,
        String note
) {
    public record Item(
            String thumbnail,
            String title,
            String slug,
            String color,
            String size,
            String priceText,
            int quantity,
            String lineTotalText
    ) {
    }

    public record Shipping(
            String recipient,
            String phone,
            String address
    ) {
    }

    public record Totals(
            String subtotalText,
            String discountText,
            String shippingText,
            String totalText
    ) {
    }
}

