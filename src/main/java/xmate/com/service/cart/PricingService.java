package xmate.com.service.cart;

import xmate.com.dto.checkout.PricingDto;

public interface PricingService {
    PricingDto calculate(Long addressId, String couponCode);
}
