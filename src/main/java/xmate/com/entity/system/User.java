// src/main/java/xmate/com/domain/system/User.java
package xmate.com.entity.system;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique = true, length = 60)
    private String username;

    @Column(nullable=false, length = 255)
    private String password;

    @Column(name="full_name", length=120)
    private String fullName;

    @Column(unique = true, length = 120)
    private String email;

    @Column(name="sdt", unique = true, length = 10)
    private String phone;

    @Column(name="lương", precision = 18, scale = 2) // cột có dấu như bạn định nghĩa
    private BigDecimal salary;

    @Column(name="is_active", nullable = false)
    private Boolean active = true;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name="user_roles",
            joinColumns=@JoinColumn(name="user_id"),
            inverseJoinColumns=@JoinColumn(name="role_id")
    )
    private Set<Role> roles;
}
