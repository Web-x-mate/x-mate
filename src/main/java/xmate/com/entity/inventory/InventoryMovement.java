package xmate.com.entity.inventory;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.system.User;
import xmate.com.entity.common.InventoryMovementType;
import xmate.com.entity.common.InventoryRefType;
@Entity @Table(name="inventory_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovement {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id", nullable=false)
    private ProductVariant variant;
    @Column(nullable=false) private Integer qty;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private InventoryMovementType type;
    @Enumerated(EnumType.STRING) @Column(name="ref_type")
    private InventoryRefType refType;
    @Column(name="ref_id") private Long refId;
    @Column(length=255) private String note;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by")
    private User createdBy;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
}
