package xmate.com.entity.procurement;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="suppliers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=160) private String name;
    @Column(length=160) private String email;
    @Column(length=50) private String phone;
    @Column(length=255) private String address;
    @Column(length=255) private String note;
}
