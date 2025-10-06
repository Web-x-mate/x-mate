package xmate.com.domain.customer;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="loyalty_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoyaltyAccount {
    @Id @Column(name="customer_id") private Long customerId;
    @OneToOne(fetch=FetchType.LAZY) @MapsId @JoinColumn(name="customer_id")
    private Customer customer;
    private Integer points = 0;
    @Column(length=50) private String tier;
    @Column(name="updated_at", nullable=false) private java.time.LocalDateTime updatedAt = java.time.LocalDateTime.now();
}
