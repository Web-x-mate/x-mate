// src/main/java/xmate/com/dto/checkout/OrderPlacedDto.java
package xmate.com.dto.checkout;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderPlacedDto(
        Long id,
        String code,
        String status,
        BigDecimal total,
        String payUrl
) {}
