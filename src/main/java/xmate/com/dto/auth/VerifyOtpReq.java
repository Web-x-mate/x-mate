package xmate.com.dto.auth;

public record VerifyOtpReq(String token, String code) {}