package xmate.com.controller.cart;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SepayWebhookController.class);

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
        log.info("[sepay.webhook] incoming headers={}, body={}",
                headers == null ? 0 : headers.size(),
                payload == null ? "{}" : payload.keySet());
        // Verification policy:
        // - If a dedicated webhook secret is configured, REQUIRE it in header.
        // - If not configured: do not hard-require a header (Sepay may not send one);
        //   but if a token is provided, accept it when matching either webhookSecret or the API key.
        String providedToken = resolveIncomingToken(headers, headerSignatureName);
        if (StringUtils.hasText(webhookSecret)) {
            if (!webhookSecret.equals(providedToken)) {
                return ResponseEntity.status(401).body(Map.of("ok", false, "error", "invalid_signature"));
            }
        } else if (StringUtils.hasText(providedToken)) {
            if (StringUtils.hasText(sepayApiKey) && !sepayApiKey.equals(providedToken)) {
                return ResponseEntity.status(401).body(Map.of("ok", false, "error", "invalid_signature"));
            }
        }

        long amount = getLong(
                payload.get("amount"),
                payload.get("transfer_amount"),         // snake_case
                payload.get("transferAmount"),          // camelCase (Sepay actual)
                payload.get("money"),
                payload.get("transferMoney"));
        String content = str(payload.get("description"));
        if (!StringUtils.hasText(content)) content = str(payload.get("content"));
        if (!StringUtils.hasText(content)) content = str(payload.get("note"));
        if (!StringUtils.hasText(content)) content = str(payload.get("message"));
        String providedCode = str(payload.get("code")); // Sepay may parse code for you when configured

        if (!StringUtils.hasText(content)) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "error", "Missing transfer content"));
        }

        String prefix = StringUtils.hasText(sepayTransferPrefix) ? sepayTransferPrefix : fallbackTransferPrefix;
        String foundCode = StringUtils.hasText(providedCode)
                ? extractOrderCode(providedCode, prefix)
                : extractOrderCode(content, prefix);
        if (!StringUtils.hasText(foundCode)) {
            log.info("[sepay.webhook] no order code in content='{}'", content);
            return ResponseEntity.ok(Map.of("ok", true, "matched", false));
        }

        Order order = orderRepository.findByCode(foundCode).orElse(null);
        if (order == null) {
            log.warn("[sepay.webhook] order not found code={}", foundCode);
            return ResponseEntity.ok(Map.of("ok", true, "matched", false, "reason", "order_not_found"));
        }

        if (amount <= 0 || amount < order.getTotal()) {
            log.warn("[sepay.webhook] amount not enough code={}, got={}, need>={}", order.getCode(), amount, order.getTotal());
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
        } catch (Exception ignored) {
            log.warn("[sepay.webhook] ws notify failed for code={}", order.getCode());
        }

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

        String token = hv.trim();
        String lower = token.toLowerCase(java.util.Locale.ROOT);
        // Accept common auth schemes used by Sepay dashboards: "Apikey <token>", "Bearer <token>", etc.
        if (lower.startsWith("bearer ")) token = token.substring(7).trim();
        else if (lower.startsWith("apikey ")) token = token.substring(7).trim();
        else if (lower.startsWith("api-key ")) token = token.substring(8).trim();
        else if (lower.startsWith("token ")) token = token.substring(6).trim();
        else if (lower.startsWith("apisecret ")) token = token.substring(10).trim();
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
                String s = String.valueOf(c).trim();
                // Try integer first
                return Long.parseLong(s);
            } catch (Exception ignored) {}
            try {
                // Remove all non-digit characters to handle formats like "200,000.00" or "200000.00"
                String digits = String.valueOf(c).replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) return Long.parseLong(digits);
            } catch (Exception ignored) {}
        }
        return 0L;
    }

    private String extractOrderCode(String content, String prefix) {
        if (content == null) return null;
        String normalized = content.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        String flat = normalized.replaceAll("[^A-Z0-9]", "");

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
                if (sb.length() > 0) {
                    String token = sb.toString();
                    // Accept separators between prefix and code: '-', ':', space or none
                    // 1) Exact prefix-<CODE>
                    String expectDash = pfx + "-";
                    if (token.startsWith(expectDash) && token.length() > expectDash.length()) {
                        return token.substring(expectDash.length());
                    }
                    // 2) Prefix with colon/space
                    String expectColon = pfx + ":";
                    if (token.startsWith(expectColon) && token.length() > expectColon.length()) {
                        return token.substring(expectColon.length());
                    }
                    String expectSpace = pfx + " ";
                    if (token.startsWith(expectSpace) && token.length() > expectSpace.length()) {
                        return token.substring(expectSpace.length());
                    }
                    // 3) No separator: try to peel prefix from start
                    if (token.startsWith(pfx) && token.length() > pfx.length()) {
                        return token.substring(pfx.length());
                    }
                    return token;
                }
            }
            // Also try matching on a hyphen-less version of the prefix within a hyphen-less content
            String pfxFlat = pfx.replaceAll("[^A-Z0-9]", "");
            int k = flat.indexOf(pfxFlat);
            if (k >= 0) {
                String after = flat.substring(k + pfxFlat.length());
                java.util.regex.Matcher mFlat = java.util.regex.Pattern
                        .compile("XM([0-9A-F]{8})")
                        .matcher(after);
                if (mFlat.find()) {
                    return "XM-" + mFlat.group(1);
                }
            }
        }

        // Fallback 1: 'XM-XXXXXXXX' (default format, hex-only)
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("([A-Z]{2}-[0-9A-F]{8})")
                .matcher(normalized);
        if (m.find()) return m.group(1);

        // Fallback 2: prefix then code separated by space/colon
        if (StringUtils.hasText(prefix)) {
            String pfx = prefix.trim().toUpperCase(Locale.ROOT);
            m = java.util.regex.Pattern
                    .compile(java.util.regex.Pattern.quote(pfx) + "[\u0020:]+([A-Z]{2}-?[0-9A-F]{8})")
                    .matcher(normalized);
            if (m.find()) return m.group(1);
        }

        // Fallback 3: hyphenless 'XMXXXXXXXX' anywhere -> rebuild to 'XM-XXXXXXXX'
        m = java.util.regex.Pattern
                .compile("XM([0-9A-F]{8})")
                .matcher(flat);
        if (m.find()) return "XM-" + m.group(1);

        // Fallback: CODE: <value>
        m = java.util.regex.Pattern.compile("CODE[:\\s]+([A-Z0-9-]{3,})").matcher(normalized);
        if (m.find()) return m.group(1);
        return null;
    }
}
