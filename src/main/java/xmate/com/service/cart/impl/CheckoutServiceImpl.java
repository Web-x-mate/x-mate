package xmate.com.service.cart.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xmate.com.dto.cart.CartDto;
import xmate.com.dto.checkout.CheckoutReq;
import xmate.com.dto.checkout.OrderPlacedDto;
import xmate.com.dto.checkout.PricingDto;
import xmate.com.entity.cart.Payment;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.enums.PaymentMethod;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;
import xmate.com.entity.customer.Address;
import xmate.com.entity.customer.Customer;

import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
//import xmate.com.repo.cart.PaymentRepository;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.customer.AddressRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.sales.OrderItemRepository;
import xmate.com.repo.sales.OrderRepository;
import xmate.com.service.cart.CartService;
import xmate.com.service.cart.CheckoutService;
import xmate.com.service.cart.PricingService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
//    private final PaymentRepository paymentRepository;
    private final CustomerRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final CartService cartService;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;

    /** Lấy customer hiện tại hoặc ném 401 để FE xử lý đăng nhập */
    private Customer currentCustomerOr401() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }
        String name = auth.getName();
        if (name == null || "anonymousUser".equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }
        return userRepository.findByEmail(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bạn chưa đăng nhập"));
    }

    @Override
    @Transactional
    public OrderPlacedDto placeOrder(CheckoutReq req, String idempotencyKey) {
        // 1) User hiện tại (401 nếu chưa đăng nhập)
        Customer user = currentCustomerOr401();

        // 2) Giỏ hàng
        CartDto currentCart = cartService.getCartForCurrentUser();
        if (currentCart.items() == null || currentCart.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giỏ hàng của bạn đang trống.");
        }

        // 3) Tính giá
        PricingDto pricing = pricingService.calculate(req.addressId(), req.couponCode());
        long total = pricing.total();

        // 4) Địa chỉ giao hàng
        Address addressToUse;
        if (req.addressId() != null) {
            addressToUse = addressRepository.findById(req.addressId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Địa chỉ đã chọn không hợp lệ."));
        } else {
            Address newAddress = new Address();
            newAddress.setCustomer(user);
            newAddress.setLine1(req.newAddressLine1());
            newAddress.setCity(req.newAddressCity());
            newAddress.setPhone(req.newAddressPhone());
            newAddress.setWard(req.newAddressWard());
            newAddress.setDistrict(req.newAddressDistrict());
            addressToUse = addressRepository.save(newAddress);
        }
        String addressJson = convertAddressToJson(addressToUse);

        // 5) Tạo đơn hàng
        Order order = new Order();
        order.setCustomer(user);
        order.setCode(generateOrderCode());
        order.setShippingAddress(addressJson);
        order.setSubtotal(pricing.subtotal());
        order.setDiscountAmount(pricing.discount());
        order.setShippingFee(pricing.shipping());
        order.setTotal(total);
        order.setNoteInternal(req.note());

        PaymentMethod paymentMethodEnum = PaymentMethod.valueOf(req.paymentMethod().toUpperCase());
        order.setStatus(paymentMethodEnum == PaymentMethod.COD
                ? OrderStatus.PLACED
                : OrderStatus.PENDING_PAYMENT);

        final Order savedOrder = orderRepository.save(order);

        // 6) Items
        List<OrderItem> orderItems = currentCart.items().stream().map(item -> {
            ProductVariant variant = variantRepository.findById(item.variantId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Không tìm thấy sản phẩm ID: " + item.variantId()));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(savedOrder);
            orderItem.setProduct(variant.getProduct());
            orderItem.setVariant(variant);
            orderItem.setProductName(item.productName());
            orderItem.setQty(item.qty());
            orderItem.setPrice(item.price());
            orderItem.setLineTotal(item.lineTotal());
            return orderItem;
        }).collect(Collectors.toList());
        orderItemRepository.saveAll(orderItems);

        // 7) Payment
        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setMethod(req.paymentMethod());
        payment.setAmount(total);
        payment.setStatus(PaymentStatus.UNPAID.name());
        payment.setTxnRef(savedOrder.getCode());
//        paymentRepository.save(payment);

        // 8) URL chuyển hướng
        String redirectUrl = (paymentMethodEnum == PaymentMethod.COD)
                ? "/orders/" + savedOrder.getCode()
                : "/orders/pay/" + savedOrder.getCode();

        return OrderPlacedDto.builder()
                .id(savedOrder.getId())
                .code(savedOrder.getCode())
                .status(savedOrder.getStatus().name())
                .total(BigDecimal.valueOf(savedOrder.getTotal()))
                .payUrl(redirectUrl)
                .build();
    }

    private String generateOrderCode() {
        return "XM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String convertAddressToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
