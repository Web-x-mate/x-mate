// src/main/java/xmate/com/service/sales/OrderService.java
package xmate.com.service.sales;

import org.springframework.data.domain.*;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;

import java.util.List;

public interface OrderService {
    Page<Order> search(String q, String status, String payment, String shipping, Pageable pageable);
    Order get(Long id);
    Order getByCode(String code);

    Order create(Order order, List<OrderItem> items);
    Order update(Long id, Order order, List<OrderItem> items);
    void delete(Long id);

    /** Tính lại subtotal/discount/fee/total dựa trên items (price*qty). */
    Order recalc(Long orderId);
}
