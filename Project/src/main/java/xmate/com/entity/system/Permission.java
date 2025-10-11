// src/main/java/xmate/com/domain/system/Permission.java
package xmate.com.entity.system;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "permissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_key", nullable = false, unique = true, length = 100)
    private String key; // ví dụ: "catalog.product.view"
}
