// src/main/java/xmate/com/domain/system/Role.java
package xmate.com.domain.system;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity @Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;            // ADMIN / MANAGER / ...

    @Column(length = 255)
    private String description;

    @ManyToMany
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions;
}
