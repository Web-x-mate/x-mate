package xmate.com.dto.checkout;

import jakarta.validation.constraints.NotNull;

public record PricingReq(
        @NotNull Long addressId,
        String couponCode
) {}
