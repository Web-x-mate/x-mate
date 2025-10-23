package xmate.com.entity.sales;// xmate.com.entity.sales.Payment
import jakarta.persistence.*;
import lombok.*;


@Entity(name = "SalesPayment")
@Table(name = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable=false)
    private long amount;

    public enum ProofStatus { SUBMITTED, APPROVED, REJECTED }

    @Column(name="proof_image")  private String proofImage;
    @Column(name="proof_note")   private String proofNote;
    @Enumerated(EnumType.STRING) @Column(name="proof_status")
    private ProofStatus proofStatus;
    @Column(name="submitted_at") private java.time.LocalDateTime submittedAt;
    @Column(name="reviewed_at")  private java.time.LocalDateTime reviewedAt;
    @Column(name="reviewed_by")  private String reviewedBy;
}
