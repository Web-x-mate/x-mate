package xmate.com.dto.auth;
import jakarta.validation.constraints.*;
public record RefreshReq(@NotBlank String refreshToken) {}
