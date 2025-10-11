// src/main/java/xmate/com/controller/cart/CheckoutApiController.java
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/checkout", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CheckoutApiController {

    private final PricingService pricingService;
    private final CheckoutService checkoutService;

    // GET "ping" cho phép test nhanh trên trình duyệt
    @GetMapping
    public Map<String, Object> info() {
        return Map.of(
                "ok", true,
                "message", "Use GET /api/checkout/pricing?addressId=&coupon=... or POST endpoints.",
                "placeOrder", "POST /api/checkout"
        );
    }

    // GET pricing: dùng query params, tiện mở trực tiếp trên browser
    @GetMapping("/pricing")
    public PricingDto pricingByQuery(@RequestParam(required = false) Long addressId,
                                     @RequestParam(name = "coupon", required = false) String couponCode) {
        return pricingService.calculate(addressId, couponCode);
    }

    // POST pricing: giữ nguyên cho client gửi JSON
    @PostMapping(value = "/pricing", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PricingDto pricing(@RequestBody PricingReq req) {
        return pricingService.calculate(req.addressId(), req.couponCode());
    }

    // Đặt hàng: BẮT BUỘC POST
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderPlacedDto place(@RequestBody CheckoutReq req,
                                @RequestHeader(value = "Idempotency-Key", required = false) String idemKey) {
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = UUID.randomUUID().toString();
        }
        return checkoutService.placeOrder(req, idemKey);
    }
}
