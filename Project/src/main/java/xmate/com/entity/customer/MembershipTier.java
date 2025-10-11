package xmate.com.entity.customer;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="membership_tier")
public class MembershipTier {

    @Id
    @Column(length = 16)
    private String code; // NEW, SILVER, GOLD, PLATINUM

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal thresholdAmount; // ngưỡng đạt hạng trong kỳ
}