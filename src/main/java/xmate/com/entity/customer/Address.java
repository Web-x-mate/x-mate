package xmate.com.entity.customer;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 120)
    private String fullName;

    private String line1;
    private String line2;
    private String ward;
    private String district;
    private String city;
    private String phone;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean defaultAddress = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
