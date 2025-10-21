package xmate.com.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record FacebookLoginReq(
        @NotBlank String accessToken   // USER access token từ FB SDK
) {}
