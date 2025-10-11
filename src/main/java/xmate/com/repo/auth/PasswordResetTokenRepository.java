package xmate.com.repo.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.auth.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {}