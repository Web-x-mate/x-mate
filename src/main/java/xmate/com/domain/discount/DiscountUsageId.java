package xmate.com.domain.discount;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class DiscountUsageId implements Serializable {
    private Long discountId;
    private Long orderId;
}
