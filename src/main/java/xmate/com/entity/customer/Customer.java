package xmate.com.entity.customer;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;
@Entity @Table(name="customers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=160) private String name;
    @Column(unique=true, length=160) private String email;
    @Column(length=30) private String phone;
    @Column(length=255) private String address;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    @ManyToMany
    @JoinTable(name="customer_segments",
       joinColumns=@JoinColumn(name="customer_id"),
       inverseJoinColumns=@JoinColumn(name="segment_id"))
    private Set<Segment> segments;
    @OneToOne(mappedBy="customer", cascade=CascadeType.ALL, orphanRemoval=true)
    private LoyaltyAccount loyaltyAccount;
}
