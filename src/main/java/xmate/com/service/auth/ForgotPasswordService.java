package xmate.com.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.dto.auth.ForgotReq;
import xmate.com.entity.auth.OtpToken;
import xmate.com.entity.auth.PasswordResetToken;
import xmate.com.repo.auth.OtpTokenRepository;
import xmate.com.repo.auth.PasswordResetTokenRepository;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.util.IdMask;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ForgotPasswordService {
    private final CustomerRepository userRepo;
    private final OtpTokenRepository otpRepo;
    private final PasswordResetTokenRepository prRepo;

    @Qualifier("emailSender")
    private final OtpSender emailSender;
    @Qualifier("smsSender")
    private final OtpSender smsSender;

    private static final SecureRandom RND = new SecureRandom();

    @Transactional
    public Map<String, String> start(ForgotReq req) {
        var raw = req.identity().trim();
        var byEmail = IdMask.looksLikeEmail(raw);
        var user = byEmail
                ? userRepo.findByEmailIgnoreCase(raw).orElse(null)
                : userRepo.findByPhone(IdMask.normalizePhone(raw)).orElse(null);

        if (user == null) throw new IllegalArgumentException("Không tìm thấy tài khoản với thông tin bạn nhập.");

        var otp = OtpToken.builder()
                .userId(user.getId())
                .purpose("FORGOT_PASSWORD")
                .channel(byEmail ? "EMAIL" : "SMS")
                .code(String.format("%06d", RND.nextInt(1_000_000)))
                .expiresAt(Instant.now().plusSeconds(300))
                .resendAvailableAt(Instant.now().plusSeconds(60))
                .attemptCount(0)
                .used(false)
                .build();
        otpRepo.save(otp);

        var body = "Mã OTP của bạn là: " + otp.getCode() + " (hết hạn sau 5 phút).";
        if (byEmail) emailSender.send(user.getEmail(), "Xác thực OTP", body);
        else smsSender.send(user.getPhone(), "", body);

        return Map.of(
                "otpId", otp.getId(),
                "channel", byEmail ? "EMAIL" : "SMS",
                "destination", byEmail ? IdMask.maskEmail(user.getEmail()) : IdMask.maskPhone(user.getPhone())
        );
    }

    @Transactional
    public String verify(String otpId, String code) {
        var otp = otpRepo.findById(otpId).orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ."));
        if (otp.isUsed()) throw new IllegalStateException("OTP đã dùng.");
        if (Instant.now().isAfter(otp.getExpiresAt())) throw new IllegalStateException("OTP đã hết hạn.");
        if (otp.getAttemptCount() >= 5) throw new IllegalStateException("Nhập sai quá 5 lần.");

        otp.setAttemptCount(otp.getAttemptCount() + 1);
        if (!otp.getCode().equals(code)) {
            otpRepo.save(otp);
            throw new IllegalArgumentException("Mã OTP không đúng.");
        }
        otp.setUsed(true);
        otpRepo.save(otp);

        var pr = PasswordResetToken.builder()
                .userId(otp.getUserId())
                .expiresAt(Instant.now().plusSeconds(900)) // 15'
                .used(false).build();
        prRepo.save(pr);
        return pr.getId();
    }

    // ====== THÊM MỚI: gửi lại OTP với rate-limit 60s ======
    @Transactional
    public Map<String, String> resend(String otpId) {
        var old = otpRepo.findById(otpId).orElseThrow(() -> new IllegalArgumentException("OTP không hợp lệ."));
        if (Instant.now().isBefore(old.getResendAvailableAt())) {
            var wait = old.getResendAvailableAt().getEpochSecond() - Instant.now().getEpochSecond();
            throw new IllegalStateException("Vui lòng đợi " + Math.max(1, wait) + " giây để gửi lại.");
        }
        var user = userRepo.findById(old.getUserId()).orElseThrow();
        var byEmail = "EMAIL".equals(old.getChannel());

        var fresh = OtpToken.builder()
                .userId(old.getUserId())
                .purpose("FORGOT_PASSWORD")
                .channel(old.getChannel())
                .code(String.format("%06d", RND.nextInt(1_000_000)))
                .expiresAt(Instant.now().plusSeconds(300))
                .resendAvailableAt(Instant.now().plusSeconds(60))
                .attemptCount(0)
                .used(false)
                .build();
        otpRepo.save(fresh);

        var body = "Mã OTP của bạn là: " + fresh.getCode() + " (hết hạn sau 5 phút).";
        if (byEmail) emailSender.send(user.getEmail(), "Xác thực OTP", body);
        else smsSender.send(user.getPhone(), "", body);

        return Map.of(
                "otpId", fresh.getId(),
                "channel", old.getChannel(),
                "destination", byEmail ? IdMask.maskEmail(user.getEmail()) : IdMask.maskPhone(user.getPhone())
        );
    }

    @Transactional
    public void reset(String token, String newPasswordHash) {
        var pr = prRepo.findById(token)
                .orElseThrow(() -> new IllegalArgumentException("Token không hợp lệ."));

        if (pr.isUsed() || Instant.now().isAfter(pr.getExpiresAt())) {
            throw new IllegalStateException("Token đã dùng hoặc đã hết hạn.");
        }

        var user = userRepo.findById(pr.getUserId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));

        // Lưu mật khẩu mới (đÃ MÃ HÓA từ controller)
        user.setPasswordHash(newPasswordHash);

        // Đánh dấu token đã sử dụng
        pr.setUsed(true);

    }
}
