package xmate.com.service.auth.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import xmate.com.service.auth.OtpSender;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress; // <-- thêm import

@Slf4j
@Component("emailSender")
@RequiredArgsConstructor
public class EmailSender implements OtpSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@localhost}")
    private String from;

    @Value("${app.mail.fromName:no-reply}")   // <-- cấu hình tên hiển thị
    private String fromName;

    @Override
    public void send(String to, String subject, String body) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            // Hiển thị: "no-reply xmate <you@gmail.com>"
            helper.setFrom(new InternetAddress(from, fromName));

            // (tuỳ chọn) nơi nhận phản hồi
            // helper.setReplyTo(new InternetAddress("support@xmate.com", "X-Mate Support"));

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // true nếu bạn gửi HTML

            mailSender.send(msg);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Send email failed", e);
            throw new RuntimeException("Không gửi được email. Vui lòng thử lại sau.");
        }
    }
}
