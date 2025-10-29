package xmate.com.dto.checkout;

public record PricingDto(
        long subtotal,
        long discount,
        long shipping,
        long total
) {}
