package xmate.com.service.auth.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xmate.com.service.auth.OtpSender;

@Slf4j
@Component("smsSender")
public class SmsSender implements OtpSender {
    @Override
    public void send(String to, String subject, String body) {
        // Tạm log để test. Sau này tích hợp nhà mạng/SMS provider tại đây.
        log.info("[DEV-SMS] to={} | {}", to, body);
    }
}
