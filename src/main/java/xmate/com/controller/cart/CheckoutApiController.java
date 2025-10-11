package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.checkout.CheckoutReq;
import xmate.com.dto.checkout.OrderPlacedDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.dto.checkout.PricingReq;
import xmate.com.service.cart.CheckoutService;
import xmate.com.service.cart.PricingService;

@RestController
@RequestMapping(value = "/api/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CheckoutApiController {

    private final PricingService pricingService;
    private final CheckoutService checkoutService;

    // ✅ Tính tổng tiền (có thể kèm mã giảm giá)
    @PostMapping(value = "/pricing", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PricingDto pricing(@RequestBody PricingReq req) {
        return pricingService.calculate(req.addressId(), req.couponCode());
    }

    // ✅ Đặt hàng
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderPlacedDto place(@RequestBody CheckoutReq req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = java.util.UUID.randomUUID().toString();
        }
        return checkoutService.placeOrder(req, idemKey);
    }
}
