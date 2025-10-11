package xmate.com.service.cart;

import org.springframework.data.domain.Page;
import xmate.com.dto.orders.OrderDetailDto;
import xmate.com.dto.orders.OrderSummaryDto;

public interface OrderQueryService {
    // Sửa lại kiểu trả về cho đúng
    Page<OrderSummaryDto> listByUser(int page, int size);

    OrderDetailDto detail(String code);

}