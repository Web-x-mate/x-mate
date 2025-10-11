package xmate.com.entity.procurement;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import xmate.com.entity.common.POStatus;
@Entity @Table(name="purchase_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true, length=40) private String code;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="supplier_id", nullable=false)
    private Supplier supplier;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private POStatus status = POStatus.DRAFT;
    @Column(name="expected_date") private LocalDate expectedDate;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="updated_at") private LocalDateTime updatedAt;
}
