package xmate.com.domain.discount;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import xmate.com.domain.common.DiscountKind;
import xmate.com.domain.common.DiscountValueType;
@Entity @Table(name="discounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Discount {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private DiscountKind type = DiscountKind.CODE;
    @Column(unique=true, length=60) private String code;
    @Enumerated(EnumType.STRING) @Column(name="value_type", nullable=false)
    private DiscountValueType valueType = DiscountValueType.PERCENT;
    @Column(name="value_amount", nullable=false, precision=12, scale=2)
    private BigDecimal valueAmount;
    @Column(name="min_order", precision=12, scale=2) private BigDecimal minOrder;
    @Column(name="conditions_json", columnDefinition="json") private String conditionsJson;
    @Column(name="start_at") private LocalDateTime startAt;
    @Column(name="end_at") private LocalDateTime endAt;
    @Column(name="usage_limit") private Integer usageLimit;
    @Column(name="used_count", nullable=false) private Integer usedCount = 0;
    @Column(nullable=false, length=10) private String status = "ACTIVE";
}
