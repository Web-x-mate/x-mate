package xmate.com.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import xmate.com.entity.customer.Customer;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private Customer customer;

    private Instant expiresAt;
    private Boolean revoked = false;
}
