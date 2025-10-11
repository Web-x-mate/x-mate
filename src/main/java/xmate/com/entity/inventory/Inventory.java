package xmate.com.entity.inventory;
import jakarta.persistence.*;
import lombok.*;
import xmate.com.entity.catalog.ProductVariant;
@Entity @Table(name="inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {
    @Id @Column(name="variant_id")
    private Long variantId;
    @OneToOne(fetch=FetchType.LAZY) @MapsId @JoinColumn(name="variant_id")
    private ProductVariant variant;
    @Column(name="qty_on_hand", nullable=false) private Integer qtyOnHand = 0;
    @Column(name="qty_reserved", nullable=false) private Integer qtyReserved = 0;
    @Column(name="updated_at", nullable=false) private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();
}
