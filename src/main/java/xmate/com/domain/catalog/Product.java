package xmate.com.domain.catalog;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import xmate.com.domain.common.ProductStatus;
import xmate.com.domain.common.Gender;
@Entity @Table(name="products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=160) private String name;

    @Column(nullable=false, unique=true, length=200) private String slug;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="category_id")
    private Category category;

    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Lob private String description;
    @Column(length=120) private String material;
    @Column(length=60) private String fit;
    @Enumerated(EnumType.STRING) private Gender gender = Gender.UNISEX;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name="updated_at") private LocalDateTime updatedAt;
}
