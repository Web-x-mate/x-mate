package xmate.com.entity.catalog;
import jakarta.persistence.*;
import lombok.*;
import xmate.com.entity.common.MediaType;
@Entity @Table(name="product_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMedia {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id", nullable=false)
    private Product product;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="variant_id")
    private ProductVariant variant;
    @Enumerated(EnumType.STRING) @Column(name="media_type", nullable=false)
    private MediaType mediaType = MediaType.IMAGE;
    @Column(nullable=false, length=500) private String url;
    @Column(name="is_primary", nullable=false) private Boolean primary = false;
    @Column(name="sort_order", nullable=false) private Integer sortOrder = 0;
}
