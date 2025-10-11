package xmate.com.repo.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.auth.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}
