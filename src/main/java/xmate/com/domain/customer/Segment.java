// src/main/java/xmate/com/domain/customer/Segment.java
package xmate.com.domain.customer;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="segments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Segment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true, length=120)
    private String name;

    private String description;

    // >>> thêm mới:
    @Column
    private Long points; // ngưỡng điểm tối thiểu (có thể null)
}
