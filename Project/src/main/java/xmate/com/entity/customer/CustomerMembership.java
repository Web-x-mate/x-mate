package xmate.com.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name="customer_membership")
public class CustomerMembership {

    /** Chia sẻ khóa chính với users.id */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /** Ràng buộc 1–1 tới User, dùng chung PK (FK = PK) */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_um_user"))
    private Customer customer;

    /** Hạng hiện tại (FK tới membership_tier.code) */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_code", referencedColumnName = "code",
            foreignKey = @ForeignKey(name = "fk_um_tier"))
    private MembershipTier tier;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** Số đã chi trong kỳ (giai đoạn này set tay để hiển thị) */
    @Column(name = "progress_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal progressAmount = BigDecimal.ZERO;

    @PrePersist
    void initDefaults() {
        if (progressAmount == null) progressAmount = BigDecimal.ZERO;
    }
}
