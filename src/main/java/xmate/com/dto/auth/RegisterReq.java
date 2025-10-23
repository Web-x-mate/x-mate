// xmate/com/dto/auth/RegisterReq.java
package xmate.com.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterReq(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(min = 2, max = 120) String fullname,
        @Pattern(regexp = "^[0-9]{9,11}$", message = "Số điện thoại 9-11 chữ số")
        String phone,
        // token do reCAPTCHA v2 trả về (g-recaptcha-response)
        String recaptchaToken
) {}
