package xmate.com.controller.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import xmate.com.entity.sales.Order;
import xmate.com.repo.sales.OrderRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class PaymentPageController {

    private final OrderRepository orderRepository;

    // New: prefer sepay.* keys; fallback to app.pay.* keys
    @Value("${app.sepay.bank-name:}")
    private String sepayBankName;
    @Value("${app.sepay.account:}")
    private String sepayAccount;
    @Value("${app.sepay.transfer-prefix:}")
    private String sepayPrefix;
    // Optional: template for Sepay QR image (compact/card/…)
    @Value("${app.sepay.qr-template:compact}")
    private String sepayQrTemplate;

    @Value("${app.pay.bank.bin:}")
    private String bankBin;
    @Value("${app.pay.bank.account:}")
    private String bankAccount;
    @Value("${app.pay.bank.name:}")
    private String bankName;
    @Value("${app.pay.transfer.prefix:XM}")
    private String transferPrefix;

    @GetMapping("/orders/pay/{code}")
    public String pay(@PathVariable String code, Model model) {
        Order order = orderRepository.findByCode(code).orElse(null);
        if (order == null) {
            return "redirect:/orders";
        }

        model.addAttribute("order", order);

        String effPrefix = notBlank(sepayPrefix) ? sepayPrefix : transferPrefix;
        String note;
        if (effPrefix == null || effPrefix.isBlank()) {
            note = order.getCode();
        } else {
            String ordCode = order.getCode() == null ? "" : order.getCode();
            if (ordCode.toUpperCase().startsWith(effPrefix.toUpperCase())) note = ordCode;
            else note = effPrefix + "-" + ordCode;
        }
        String encodedNote = URLEncoder.encode(note, StandardCharsets.UTF_8);

        String qrUrl = null;
        String effAccount = notBlank(sepayAccount) ? sepayAccount : bankAccount;
        String effBankName = notBlank(sepayBankName) ? sepayBankName : bankName;
        String effBin = notBlank(bankBin) ? bankBin : resolveBinByBankName(effBankName);

        // Prefer Sepay's QR endpoint when Sepay bank/account is configured
        if (notBlank(effAccount) && notBlank(effBankName)) {
            // Sepay expects bank code like BIDV/VCB… in `bank`, account in `acc`, content in `des`
            String qrSepay = String.format(
                    "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=%s&download=0",
                    URLEncoder.encode(effAccount, StandardCharsets.UTF_8),
                    URLEncoder.encode(effBankName, StandardCharsets.UTF_8),
                    order.getTotal(),
                    encodedNote,
                    URLEncoder.encode(notBlank(sepayQrTemplate) ? sepayQrTemplate : "compact", StandardCharsets.UTF_8)
            );
            qrUrl = qrSepay;
        }
        // Fallback to VietQR if Sepay QR cannot be built
        if (qrUrl == null && notBlank(effBin) && notBlank(effAccount)) {
            String accountNameParam = notBlank(effBankName)
                    ? ("&accountName=" + URLEncoder.encode(effBankName, StandardCharsets.UTF_8))
                    : "";
            qrUrl = String.format(
                    "https://img.vietqr.io/image/%s-%s-compact2.jpg?amount=%d&addInfo=%s%s",
                    effBin, effAccount, order.getTotal(), encodedNote, accountNameParam
            );
        }

        model.addAttribute("vietQrUrl", qrUrl);
        model.addAttribute("transferNote", note);
        return "order-user/payment-qr";
    }

    @GetMapping("/orders/thank-you/{code}")
    public String thankYou(@PathVariable String code, Model model) {
        Order order = orderRepository.findByCode(code).orElse(null);
        if (order == null) {
            return "redirect:/";
        }
        model.addAttribute("orderCode", code);
        model.addAttribute("orderStatus", order.getStatus().name());
        return "order-user/detail";
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // Minimal BIN mapping for common banks (extend as needed)
    private static String resolveBinByBankName(String name) {
        if (name == null) return null;
        String n = name.trim().toUpperCase();
        // BIDV (Ngân hàng Đầu tư và Phát triển Việt Nam)
        if (n.contains("BIDV") || n.contains("DAU TU") || n.contains("D\u1EA6U T\u01AF")
                || n.contains("\u0110\u1EA6U T\u01AF") || n.contains("DAU TU VA PHAT TRIEN")) {
            return "970418";
        }
        // Vietcombank
        if (n.contains("VIETCOMBANK") || n.contains("VCB") || n.contains("NGOAI THUONG")) {
            return "970436";
        }
        // Techcombank
        if (n.contains("TECHCOMBANK") || n.contains("TCB")) {
            return "970407";
        }
        // VietinBank
        if (n.contains("VIETIN") || n.contains("CONG THUONG")) {
            return "970415";
        }
        return null;
    }
}
