package xmate.com.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="password_reset_tokens", indexes={
        @Index(columnList="userId"), @Index(columnList="expiresAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Long userId;
    private Instant expiresAt;
    private boolean used;
}
