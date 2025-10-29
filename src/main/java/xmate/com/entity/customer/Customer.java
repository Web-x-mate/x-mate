package xmate.com.entity.customer;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "customers",
        indexes = {
                @Index(name = "ix_customers_email", columnList = "email"),
                @Index(name = "ix_customers_oauth", columnList = "oauth_provider,oauth_subject")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customers_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_customers_oauth_pair", columnNames = {"oauth_provider", "oauth_subject"})
        }
)
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email duy nhất (đã normalize xuống lowercase ở prePersist/preUpdate) */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    /** Cho phép null để hỗ trợ tài khoản OAuth2 không có mật khẩu cục bộ */
    @Column(name = "password_hash", nullable = true, length = 255)
    private String passwordHash;

    @Column(length = 120)
    private String fullname;

    @Column(length = 20)
    private String phone;

    /** yyyy-MM-dd */
    @Column
    private LocalDate dob;

    /** "M","F","O" */
    @Column(length = 1)
    private String gender;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    /** OAuth2 provider (vd: "google", "facebook"), có thể null nếu local account */
    @Column(name = "oauth_provider", length = 40)
    private String oauthProvider;

    /** Subject/ID của provider (vd: Google sub), có thể null nếu local account */
    @Column(name = "oauth_subject", length = 128)
    private String oauthSubject;
    @Column(name = "token_user", length = 160, unique = true)
    private String tokenUser;

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
        if (oauthProvider != null) oauthProvider = oauthProvider.trim().toLowerCase();
        if (oauthSubject != null) oauthSubject = oauthSubject.trim();
        if (tokenUser != null) tokenUser = tokenUser.trim();
    }
}
