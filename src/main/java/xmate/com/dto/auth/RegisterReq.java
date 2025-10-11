package xmate.com.dto.auth;
import jakarta.validation.constraints.*;
public record RegisterReq(
        @Email @NotBlank String email,
        @NotBlank String password,
        @NotBlank String fullname,
        String phone
) {}
