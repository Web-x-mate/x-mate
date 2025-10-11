package xmate.com.repo.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.auth.OtpToken;

public interface OtpTokenRepository extends JpaRepository<OtpToken, String> {}