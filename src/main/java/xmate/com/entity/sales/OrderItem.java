package xmate.com.entity.sales;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import xmate.com.entity.catalog.ProductVariant;
@Entity @Table(name="order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id", nullable=false)
    private Order order;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", nullable=false)
    private ProductVariant variant;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
    @Column(nullable=false) private Integer qty;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal total;
}
