package xmate.com.entity.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xmate.com.entity.catalog.ProductVariant;

@Entity @Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"cart_id","variant_id"}))
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY) private Cart cart;
    @ManyToOne(fetch = FetchType.LAZY) private ProductVariant variant;

    private int qty;

    @Column(name = "price_snap")
    private Long priceSnap;
}
