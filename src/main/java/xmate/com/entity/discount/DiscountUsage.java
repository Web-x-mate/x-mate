package xmate.com.entity.discount;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import xmate.com.entity.sales.Order;
import xmate.com.entity.customer.Customer;
@Entity @Table(name="discount_usages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscountUsage {
    @EmbeddedId private DiscountUsageId id;
    @ManyToOne(fetch=FetchType.LAZY) @MapsId("discountId") @JoinColumn(name="discount_id")
    private Discount discount;
    @OneToOne(fetch=FetchType.LAZY) @MapsId("orderId") @JoinColumn(name="order_id")
    private Order order;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="customer_id")
    private Customer customer;
    @Column(name="used_at", nullable=false) private LocalDateTime usedAt = LocalDateTime.now();
}
