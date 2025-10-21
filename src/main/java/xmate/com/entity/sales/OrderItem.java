package xmate.com.entity.sales;

import jakarta.persistence.*;
import lombok.*;
import xmate.com.entity.catalog.Product;
import xmate.com.entity.catalog.ProductVariant;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(nullable = true, length = 255)
    private String productName;

    @Column(nullable = false)
    private int qty;

    // ===== Tiền tệ: long (VND)
    @Column(nullable = false)
    private long price;

    @Column(name = "line_total", nullable = false)
    private long lineTotal;
}
