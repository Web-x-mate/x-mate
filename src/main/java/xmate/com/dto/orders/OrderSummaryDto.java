package xmate.com.dto.orders;

public record OrderSummaryDto(
        Long id,
        String code,
        java.time.LocalDateTime createdAt,
        long total,
        String status
) {}