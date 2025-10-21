package xmate.com.entity.sales;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // ====== ENUMS ======
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // Nếu chưa cần ShippingStatus thì tạm thời để NULLABLE cho an toàn
    @Column(name = "shipping_status", length = 50, nullable = true)
    private String shippingStatus = "NOT_SHIPPED";

    // ====== TIỀN TỆ ======
    @Column(nullable = false)
    private long subtotal = 0L;

    @Column(name = "discount_amount", nullable = false)
    private long discountAmount = 0L;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee = 0L;

    @Column(nullable = false)
    private long total = 0L;

    // ====== GIAO HÀNG & GHI CHÚ ======
    @Column(name = "shipping_address", length = 255)
    private String shippingAddress;

    @Column(name = "shipping_provider", length = 120)
    private String shippingProvider;

    @Column(name = "tracking_code", length = 120)
    private String trackingCode;

    @Column(name = "note_internal", length = 255)
    private String noteInternal;

    // ====== NGÀY TẠO / CẬP NHẬT ======
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ====== LIÊN KẾT SẢN PHẨM ======
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OrderItem> items;



}
