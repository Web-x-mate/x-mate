package xmate.com.dto.orders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

public record OrderDetailDto(
        Long id,
        String code,
        String status,
        Instant createdAt,
        long subtotal,
        long discount,
        long shippingFee,
        long total,
        AddressSnapDto addressSnap,
        List<OrderItemDto> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AddressSnapDto(
            String fullName,
            String phone,
            String fullAddress
    ) {}

    public record OrderItemDto(
            String productName,
            int qty,
            long price,
            Object lineTotal
    ) {}
}