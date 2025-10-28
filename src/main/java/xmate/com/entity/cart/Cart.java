package xmate.com.entity.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xmate.com.entity.customer.Customer;

@Entity
@Getter
@Setter
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private Customer user;

    private String appliedCouponCode;

    @Column(name = "guest_key", unique = true, length = 64)
    private String guestKey;
}
