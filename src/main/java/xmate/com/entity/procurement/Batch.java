package xmate.com.entity.procurement;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import xmate.com.entity.catalog.ProductVariant;
@Entity @Table(name="batches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Batch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", nullable=false)
    private ProductVariant variant;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="po_item_id")
    private PurchaseOrderItem purchaseOrderItem;
    @Column(name="unit_cost", nullable=false, precision=12, scale=2) private BigDecimal unitCost;
    @Column(name="qty_received", nullable=false) private Integer qtyReceived;
    @Column(name="received_at", nullable=false) private LocalDateTime receivedAt = LocalDateTime.now();
}
