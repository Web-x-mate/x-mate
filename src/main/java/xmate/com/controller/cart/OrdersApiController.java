package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import xmate.com.dto.orders.OrderDetailDto;
import xmate.com.dto.orders.OrderSummaryDto;
import xmate.com.service.cart.OrderQueryService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersApiController {
    private final OrderQueryService orderQueryService;

    @GetMapping public Page<OrderSummaryDto> list(@RequestParam(defaultValue="0") int page,
                                                  @RequestParam(defaultValue="10") int size){
        return orderQueryService.listByUser(page, size);
    }
    @GetMapping("/{code}") public OrderDetailDto detail(@PathVariable String code){ return orderQueryService.detail(code); }
}
