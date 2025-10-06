package xmate.com.domain.catalog;
import jakarta.persistence.*;
import lombok.*;
import java.util.Set;
@Entity @Table(name="categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, length=120) private String name;
    @Column(nullable=false, unique=true, length=160) private String slug;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="parent_id")
    private Category parent;
    @OneToMany(mappedBy="parent") private Set<Category> children;
    @Column(name="is_active", nullable=false) private Boolean active = true;
}
