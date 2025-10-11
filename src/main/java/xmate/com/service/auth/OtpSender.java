package xmate.com.service.auth;

public interface OtpSender { void send(String to, String subject, String body); }
