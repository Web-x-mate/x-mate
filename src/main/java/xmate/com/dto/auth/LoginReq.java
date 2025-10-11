package xmate.com.dto.auth;
import jakarta.validation.constraints.*;
public record LoginReq(
        @Email @NotBlank String email,
        @NotBlank String password
) {}
