package xmate.com.domain.procurement;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import xmate.com.domain.catalog.ProductVariant;
@Entity @Table(name="purchase_order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="po_id", nullable=false)
    private PurchaseOrder purchaseOrder;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", nullable=false)
    private ProductVariant variant;
    @Column(nullable=false) private Integer qty;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal cost;
    @Column(name="received_qty", nullable=false) private Integer receivedQty = 0;
}
