package xmate.com.entity.sales;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.common.OrderStatus;
import xmate.com.entity.common.PaymentStatus;
import xmate.com.entity.common.ShippingStatus;
@Entity @Table(name="orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=40) private String code;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id")
    private Customer customer;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private OrderStatus status = OrderStatus.PENDING;
    @Enumerated(EnumType.STRING) @Column(name="payment_status", nullable=false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    @Enumerated(EnumType.STRING) @Column(name="shipping_status", nullable=false)
    private ShippingStatus shippingStatus = ShippingStatus.NOT_SHIPPED;
    @Column(precision=12, scale=2, nullable=false) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name="discount_amount", precision=12, scale=2, nullable=false) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name="shipping_fee", precision=12, scale=2, nullable=false) private BigDecimal shippingFee = BigDecimal.ZERO;
    @Column(precision=12, scale=2, nullable=false) private BigDecimal total = BigDecimal.ZERO;
    @Column(name="shipping_address", length=255) private String shippingAddress;
    @Column(name="shipping_provider", length=120) private String shippingProvider;
    @Column(name="tracking_code", length=120) private String trackingCode;
    @Column(name="note_internal", length=255) private String noteInternal;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @OneToMany(mappedBy="order", cascade=CascadeType.ALL, orphanRemoval=true)
    private Set<OrderItem> items;
}
