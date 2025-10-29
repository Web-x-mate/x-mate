package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.cart.CartItemDto;
import xmate.com.dto.cart.QuickAddItemReq;
import xmate.com.service.cart.CartService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cart/items")
@RequiredArgsConstructor
public class CartQuickController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> quickAdd(@RequestBody QuickAddItemReq req) {
        Map<String, Object> body = new HashMap<>();

        Long variantId = req.variantId();
        if (variantId == null) {
            body.put("success", false);
            body.put("message", "Thiếu biến thể sản phẩm.");
            return ResponseEntity.badRequest().body(body);
        }

        int qty = req.quantity() == null || req.quantity() <= 0 ? 1 : req.quantity();

        try {
            CartDto cart = cartService.addItem(variantId, qty);
            int totalQty = cart.items().stream()
                    .mapToInt(CartItemDto::qty)
                    .sum();

            body.put("success", true);
            body.put("cartQuantity", totalQty);
            return ResponseEntity.ok(body);
        } catch (IllegalStateException ex) {
            log.warn("[CART QUICK ADD] Unauthorized: {}", ex.getMessage());
            body.put("success", false);
            body.put("message", "Vui lòng đăng nhập để mua hàng.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        } catch (IllegalArgumentException ex) {
            log.warn("[CART QUICK ADD] Bad request: {}", ex.getMessage());
            body.put("success", false);
            body.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception ex) {
            log.error("[CART QUICK ADD] Unexpected error", ex);
            body.put("success", false);
            body.put("message", "Có lỗi xảy ra. Vui lòng thử lại sau.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}

