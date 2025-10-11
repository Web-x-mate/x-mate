package xmate.com.dto.auth;

public record ResetPasswordReq(String token, String newPassword) {}