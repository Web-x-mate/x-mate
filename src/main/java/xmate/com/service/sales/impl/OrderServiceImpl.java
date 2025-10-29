// src/main/java/xmate/com/service/sales/impl/OrderServiceImpl.java
package xmate.com.service.sales.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.sales.OrderItemRepository;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.service.sales.OrderService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<Order> search(String q, String status, String payment, String shipping, Pageable pageable) {
        xmate.com.entity.enums.OrderStatus   st   = parseEnum(status,  xmate.com.entity.enums.OrderStatus.class);
        xmate.com.entity.enums.PaymentStatus pay  = parseEnum(payment, xmate.com.entity.enums.PaymentStatus.class);
        xmate.com.entity.common.ShippingStatus ship = parseEnum(shipping, xmate.com.entity.common.ShippingStatus.class);
        return orderRepo.search(q, st, pay, ship, pageable);
    }


    // helper generic
    private static <E extends Enum<E>> E parseEnum(String raw, Class<E> type) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Order get(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Order getByCode(String code) {
        return orderRepo.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    public Order create(Order order, List<OrderItem> items) {
        // Gen code nếu chưa có / kiểm tra trùng
        if (order.getCode() == null || order.getCode().isBlank()) {
            order.setCode(genCode());
        } else if (orderRepo.findByCode(order.getCode()).isPresent()) {
            throw new IllegalArgumentException("Order code already exists");
        }

        order.setId(null);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(null);

        // Mặc định tiền = 0L
        // (các field này là primitive long trong entity nên thực tế đã = 0)
        if (order.getSubtotal() < 0) order.setSubtotal(0L);
        if (order.getDiscountAmount() < 0) order.setDiscountAmount(0L);
        if (order.getShippingFee() < 0) order.setShippingFee(0L);
        if (order.getTotal() < 0) order.setTotal(0L);

        Order saved = orderRepo.save(order);

        // Lưu items
        if (items != null) {
            for (OrderItem it : items) {
                it.setId(null);
                it.setOrder(saved);
                if (it.getPrice() < 0) it.setPrice(0L);
                if (it.getQty() < 0) it.setQty(0);
                it.setLineTotal(it.getPrice() * (long) it.getQty());
                itemRepo.save(it);
            }
        }

        // Tính lại tổng
        return recalc(saved.getId());
    }

    @Override
    public Order update(Long id, Order order, List<OrderItem> items) {
        Order db = get(id);

        // Cho phép chỉnh các field
        db.setCustomer(order.getCustomer());
        db.setStatus(order.getStatus());
        db.setPaymentStatus(order.getPaymentStatus());
        db.setShippingStatus(order.getShippingStatus());
        db.setShippingAddress(order.getShippingAddress());
        db.setShippingProvider(order.getShippingProvider());
        db.setTrackingCode(order.getTrackingCode());
        db.setNoteInternal(order.getNoteInternal());

        // Fee/discount do admin nhập (long)
        db.setDiscountAmount(Math.max(0L, order.getDiscountAmount()));
        db.setShippingFee(Math.max(0L, order.getShippingFee()));
        db.setUpdatedAt(LocalDateTime.now());

        // Cập nhật items: xoá hết + thêm lại
        itemRepo.deleteByOrderId(db.getId());
        if (items != null) {
            for (OrderItem it : items) {
                it.setId(null);
                it.setOrder(db);
                if (it.getPrice() < 0) it.setPrice(0L);
                if (it.getQty() < 0) it.setQty(0);
                it.setLineTotal(it.getPrice() * (long) it.getQty());
                itemRepo.save(it);
            }
        }

        orderRepo.save(db);
        return recalc(db.getId());
    }

    @Override
    public void delete(Long id) {
        itemRepo.deleteByOrderId(id);
        orderRepo.deleteById(id);
    }

    @Override
    public Order recalc(Long orderId) {
        Order db = get(orderId);
        List<OrderItem> items = itemRepo.findByOrderId(orderId);

        long subtotal = items.stream()
                .mapToLong(i -> i.getPrice() * (long) i.getQty())
                .sum();

        long discount = Math.max(0L, db.getDiscountAmount());
        long shipFee  = Math.max(0L, db.getShippingFee());

        long total = subtotal - discount + shipFee;
        if (total < 0L) total = 0L;

        db.setSubtotal(subtotal);
        db.setTotal(total);
        db.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(db);
    }

    private String genCode() {
        // Ví dụ: ORD-YYYYMMDD-xxxx
        String date = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rnd = String.valueOf((int) (Math.random() * 9000) + 1000);
        return "ORD-" + date + "-" + rnd;
    }
}
