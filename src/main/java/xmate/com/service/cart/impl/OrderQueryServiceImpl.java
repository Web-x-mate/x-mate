package xmate.com.service.cart.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import xmate.com.dto.orders.OrderDetailDto;
import xmate.com.dto.orders.OrderSummaryDto;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.sales.Order;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.service.cart.OrderQueryService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final CustomerRepository userRepository;
    private final ObjectMapper objectMapper;

    private Customer getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng. Vui lòng đăng nhập lại."));
    }

    @Override
    public Page<OrderSummaryDto> listByUser(int page, int size) {
        Customer currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Order> orderPage = orderRepository.findByCustomer(currentUser, pageable);

        return orderPage.map(order -> new OrderSummaryDto(
                order.getId(),
                order.getCode(),
                // LocalDateTime -> Instant cho DTO
                order.getCreatedAt() == null ? null
                        : LocalDateTime.from(order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()),
                // Các trường tiền tệ là long primitive -> dùng trực tiếp, không check null
                order.getTotal(),
                order.getStatus().name()
        ));
    }

    @Override
    public OrderDetailDto detail(String code) {
        Customer currentUser = getCurrentUser();

        Order order = orderRepository.findByCodeAndCustomer(code, currentUser).orElse(null);
        if (order == null) return null;

        List<OrderDetailDto.OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderDetailDto.OrderItemDto(
                        item.getProductName(),
                        item.getQty(),
                        item.getPrice(),      // long
                        item.getLineTotal()   // long
                ))
                .collect(Collectors.toList());

        OrderDetailDto.AddressSnapDto addressSnapDto;
        try {
            // JSON địa chỉ lưu ở shippingAddress
            addressSnapDto = objectMapper.readValue(
                    order.getShippingAddress(),
                    OrderDetailDto.AddressSnapDto.class
            );
        } catch (IOException e) {
            addressSnapDto = new OrderDetailDto.AddressSnapDto("Lỗi địa chỉ", "", "");
        }

        return new OrderDetailDto(
                order.getId(),
                order.getCode(),
                order.getStatus().name(),
                order.getCreatedAt() == null ? null
                        : order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getShippingFee(),
                order.getTotal(),
                addressSnapDto,
                itemDtos
        );
    }
}
