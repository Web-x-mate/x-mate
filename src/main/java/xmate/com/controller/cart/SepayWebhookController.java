package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import xmate.com.entity.enums.OrderStatus;
import xmate.com.entity.enums.PaymentStatus;
import xmate.com.entity.sales.Order;
import xmate.com.repo.sales.OrderRepository;

import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/sepay")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate ws;

    @Value("${sepay.webhook.header-signature:Authorization}")
    private String headerSignatureName;
    @Value("${sepay.webhook.secret:}")
    private String webhookSecret;
    @Value("${app.sepay.api-key:}")
    private String sepayApiKey;
    @Value("${app.sepay.transfer-prefix:}")
    private String sepayTransferPrefix;
    @Value("${app.pay.transfer.prefix:XM}")
    private String fallbackTransferPrefix;

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Map<String, Object>> webhook(@RequestBody Map<String, Object> payload,
                                                       @RequestHeader Map<String, String> headers) {
        // Verify: use webhookSecret if present, otherwise fallback to app.sepay.api-key
        String expected = StringUtils.hasText(webhookSecret) ? webhookSecret : sepayApiKey;
        if (StringUtils.hasText(expected)) {
            String token = resolveIncomingToken(headers, headerSignatureName);
            if (!expected.equals(token)) {
                return ResponseEntity.status(401).body(Map.of("ok", false, "error", "invalid_signature"));
            }
        }

        long amount = getLong(payload.get("amount"),
                getLong(payload.get("transfer_amount"), getLong(payload.get("money"), 0)));
        String content = str(payload.get("description"));
        if (!StringUtils.hasText(content)) content = str(payload.get("content"));
        if (!StringUtils.hasText(content)) content = str(payload.get("note"));
        if (!StringUtils.hasText(content)) content = str(payload.get("message"));

        if (!StringUtils.hasText(content)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing transfer content"));
        }

        String prefix = StringUtils.hasText(sepayTransferPrefix) ? sepayTransferPrefix : fallbackTransferPrefix;
        String foundCode = extractOrderCode(content, prefix);
        if (!StringUtils.hasText(foundCode)) {
            return ResponseEntity.ok(Map.of("ok", true, "matched", false));
        }

        Order order = orderRepository.findByCode(foundCode).orElse(null);
        if (order == null) {
            return ResponseEntity.ok(Map.of("ok", true, "matched", false, "reason", "order_not_found"));
        }

        if (amount <= 0 || amount < order.getTotal()) {
            return ResponseEntity.ok(Map.of("ok", true, "matched", false, "reason", "amount_not_enough"));
        }

        boolean changed = false;
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                order.setStatus(OrderStatus.PLACED);
            }
            orderRepository.save(order);
            changed = true;
        }

        // Phát tín hiệu paid để client đang mở trang QR cập nhật UI/redirect
        try {
            ws.convertAndSend("/topic/order." + order.getCode(), Map.of(
                    "event", "paid",
                    "code", order.getCode(),
                    "paid", order.getPaymentStatus() == PaymentStatus.PAID,
                    "changed", changed,
                    "ts", System.currentTimeMillis()
            ));
        } catch (Exception ignored) {}

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "matched", true,
                "code", order.getCode(),
                "orderId", order.getId()
        ));
    }

    private static String getHeaderIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) return null;
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
        }
        return null;
    }

    private static String resolveIncomingToken(Map<String, String> headers, String preferHeader) {
        // Priority: configured header -> Authorization -> X-Api-Key -> X-Sepay-Api-Key
        String hv = null;
        if (StringUtils.hasText(preferHeader)) hv = getHeaderIgnoreCase(headers, preferHeader);
        if (!StringUtils.hasText(hv)) hv = getHeaderIgnoreCase(headers, "Authorization");
        if (!StringUtils.hasText(hv)) hv = getHeaderIgnoreCase(headers, "X-Api-Key");
        if (!StringUtils.hasText(hv)) hv = getHeaderIgnoreCase(headers, "X-Sepay-Api-Key");
        if (!StringUtils.hasText(hv)) hv = getHeaderIgnoreCase(headers, "X-Sepay-Signature");
        if (!StringUtils.hasText(hv)) return null;
        String token = hv;
        if (token != null && token.toLowerCase().startsWith("bearer ")) token = token.substring(7).trim();
        return token;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long getLong(Object... candidates) {
        for (Object c : candidates) {
            if (c == null) continue;
            if (c instanceof Number n) return n.longValue();
            try {
                return Long.parseLong(String.valueOf(c));
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private String extractOrderCode(String content, String prefix) {
        if (content == null) return null;
        String normalized = content.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);

        // If prefix provided (e.g., XMATE-ODR), capture the full token starting at prefix
        if (StringUtils.hasText(prefix)) {
            String pfx = prefix.trim().toUpperCase(Locale.ROOT);
            int i = normalized.indexOf(pfx);
            if (i >= 0) {
                int j = i;
                StringBuilder sb = new StringBuilder();
                while (j < normalized.length()) {
                    char ch = normalized.charAt(j);
                    if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '-') {
                        sb.append(ch);
                        j++;
                    } else break;
                }
                if (sb.length() > 0) return sb.toString();
            }
        }

        // Fallback: find the longest token that looks like a code with dashes
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([A-Z0-9]{2,}-[A-Z0-9-]{2,})")
                .matcher(normalized);
        if (m.find()) return m.group(1);

        // Fallback: CODE: <value>
        m = java.util.regex.Pattern.compile("CODE[:\\s]+([A-Z0-9-]{3,})").matcher(normalized);
        if (m.find()) return m.group(1);
        return null;
    }
}
