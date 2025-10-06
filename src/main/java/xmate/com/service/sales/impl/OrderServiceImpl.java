// src/main/java/xmate/com/service/sales/impl/OrderServiceImpl.java
package xmate.com.service.sales.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.sales.Order;
import xmate.com.domain.sales.OrderItem;
import xmate.com.repo.sales.OrderItemRepository;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.service.sales.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;

    @Override @Transactional(readOnly = true)
    public Page<Order> search(String q, String status, String payment, String shipping, Pageable pageable) {
        return orderRepo.search(q, status, payment, shipping, pageable);
    }

    @Override @Transactional(readOnly = true)
    public Order get(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override @Transactional(readOnly = true)
    public Order getByCode(String code) {
        return orderRepo.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    public Order create(Order order, List<OrderItem> items) {
        // sinh code nếu chưa có
        if (order.getCode() == null || order.getCode().isBlank()) {
            order.setCode(genCode());
        } else if (orderRepo.findByCode(order.getCode()).isPresent()) {
            throw new IllegalArgumentException("Order code already exists");
        }
        order.setId(null);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(null);

        // init số tiền an toàn
        if (order.getSubtotal() == null) order.setSubtotal(BigDecimal.ZERO);
        if (order.getDiscountAmount() == null) order.setDiscountAmount(BigDecimal.ZERO);
        if (order.getShippingFee() == null) order.setShippingFee(BigDecimal.ZERO);
        if (order.getTotal() == null) order.setTotal(BigDecimal.ZERO);

        Order saved = orderRepo.save(order);

        // save items
        if (items != null) {
            for (OrderItem it : items) {
                it.setId(null);
                it.setOrder(saved);
                if (it.getPrice() == null) it.setPrice(BigDecimal.ZERO);
                if (it.getQty() == null) it.setQty(0);
                it.setTotal(it.getPrice().multiply(BigDecimal.valueOf(it.getQty())));
                itemRepo.save(it);
            }
        }
        // recalc totals
        return recalc(saved.getId());
    }

    @Override
    public Order update(Long id, Order order, List<OrderItem> items) {
        Order db = get(id);

        // cho phép đổi vài field
        db.setCustomer(order.getCustomer());
        db.setStatus(order.getStatus());
        db.setPaymentStatus(order.getPaymentStatus());
        db.setShippingStatus(order.getShippingStatus());
        db.setShippingAddress(order.getShippingAddress());
        db.setShippingProvider(order.getShippingProvider());
        db.setTrackingCode(order.getTrackingCode());
        db.setNoteInternal(order.getNoteInternal());

        // fee/discount chỉnh tay (nếu có)
        db.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        db.setShippingFee(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
        db.setUpdatedAt(LocalDateTime.now());

        // cập nhật items: xoá hết + thêm lại (đơn giản, dễ hiểu)
        itemRepo.deleteByOrderId(db.getId());
        if (items != null) {
            for (OrderItem it : items) {
                it.setId(null);
                it.setOrder(db);
                if (it.getPrice() == null) it.setPrice(BigDecimal.ZERO);
                if (it.getQty() == null) it.setQty(0);
                it.setTotal(it.getPrice().multiply(BigDecimal.valueOf(it.getQty())));
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

        BigDecimal subtotal = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = db.getDiscountAmount() != null ? db.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal shipFee  = db.getShippingFee()   != null ? db.getShippingFee()   : BigDecimal.ZERO;

        BigDecimal total = subtotal.subtract(discount).add(shipFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        db.setSubtotal(subtotal);
        db.setTotal(total);
        db.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(db);
    }

    private String genCode() {
        // Ví dụ: ORD-YYYYMMDD-xxxx
        String date = java.time.LocalDate.now().toString().replaceAll("-", "");
        String rnd = String.valueOf((int)(Math.random()*9000)+1000);
        return "ORD-" + date + "-" + rnd;
    }
}
