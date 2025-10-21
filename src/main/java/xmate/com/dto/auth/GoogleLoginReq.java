package xmate.com.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginReq(@NotBlank String idToken) {}
