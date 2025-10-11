package xmate.com.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(
        name = "customers",
        indexes = {
                @Index(name = "ix_customers_email", columnList = "email")
        }
)
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 120)
    private String fullname;

    @Column(length = 20)
    private String phone;

    @Column
    private LocalDate dob;                   // yyyy-MM-dd

    /** "M","F","O" */
    @Column(length = 1)
    private String gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;


    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        normalize();
    }

    @PreUpdate
    public void preUpdate() {
        normalize();
    }

    private void normalize() {
        if (email != null) email = email.trim().toLowerCase();
        if (phone != null) phone = phone.trim();
        if (gender != null) gender = gender.trim().toUpperCase(); // "M","F","O"
    }
}
