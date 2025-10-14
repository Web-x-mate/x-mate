package xmate.com.entity.catalog;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import xmate.com.entity.common.StockPolicy;
@Entity @Table(name="product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id", nullable=false)
    private Product product;
    @Column(nullable=false, unique=true, length=64) private String sku;
    @Column(length=60) private String color;
    @Column(length=32) private String size;
    @Column(length=64) private String barcode;
    @Column(nullable=false, precision=12, scale=2) private BigDecimal price;
    @Column(name="compare_at_price", precision=12, scale=2) private BigDecimal compareAtPrice;
    @Column(precision=12, scale=2) private BigDecimal cost;
    @Column(name="weight_gram") private Integer weightGram;
    @Enumerated(EnumType.STRING) @Column(name="stock_policy", nullable=false)
    private StockPolicy stockPolicy = StockPolicy.DENY;
    @Column(name="is_active", nullable=false) private Boolean active = true;
    @Column(name="created_at", nullable=false) private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();



}
