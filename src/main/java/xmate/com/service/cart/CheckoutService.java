package xmate.com.service.cart;

import xmate.com.dto.checkout.CheckoutReq;
import xmate.com.dto.checkout.OrderPlacedDto;

public interface CheckoutService {
    OrderPlacedDto placeOrder(CheckoutReq req, String idempotencyKey);
}
