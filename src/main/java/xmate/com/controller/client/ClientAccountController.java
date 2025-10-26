package xmate.com.controller.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.discount.Discount;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.discount.DiscountRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class ClientAccountController {

    private final CustomerRepository customerRepository;
    private final DiscountRepository discountRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        String redirect = redirectIfIncomplete(me);
        if (redirect != null) return redirect;
        populateCommonModel(model, auth, me, "profile");
        model.addAttribute("mb", null);
        model.addAttribute("wallet", null);
        model.addAttribute("pageTitle", "Tài khoản của tôi");
        return "client/account/profile";
    }

    @GetMapping("/orders")
    public String orders(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        String redirect = redirectIfIncomplete(me);
        if (redirect != null) return redirect;
        populateCommonModel(model, auth, me, "orders");
        model.addAttribute("pageTitle", "Lịch sử đơn hàng");
        return "client/account/orders";
    }

    @GetMapping("/vouchers")
    public String vouchers(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        String redirect = redirectIfIncomplete(me);
        if (redirect != null) return redirect;
        populateCommonModel(model, auth, me, "vouchers");
        List<Discount> coupons = discountRepository.findActiveByKind(LocalDateTime.now(), DiscountKind.CODE);
        List<VoucherView> voucherViews = coupons.stream()
                .map(this::toVoucherView)
                .collect(Collectors.toList());
        model.addAttribute("vouchers", voucherViews);
        model.addAttribute("pageTitle", "Ví voucher");
        return "client/account/vouchers";
    }

    @GetMapping("/addresses")
    public String addresses(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        String redirect = redirectIfIncomplete(me);
        if (redirect != null) return redirect;
        populateCommonModel(model, auth, me, "addresses");
        model.addAttribute("addresses", List.of());
        model.addAttribute("pageTitle", "Sổ địa chỉ");
        return "client/account/addresses";
    }

    @GetMapping("/reviews")
    public String reviews(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        String redirect = redirectIfIncomplete(me);
        if (redirect != null) return redirect;
        populateCommonModel(model, auth, me, "reviews");
        model.addAttribute("pageTitle", "Đánh giá & phản hồi");
        return "client/account/reviews";
    }

    private Customer requireCustomer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Customer me = resolveCustomer(auth);
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được tài khoản");
        }
        return me;
    }

    private String redirectIfIncomplete(Customer me) {
        if (me.getPhone() == null || me.getPhone().isBlank()) {
            return "redirect:/auth/complete";
        }
        return null;
    }

    private Customer resolveCustomer(Authentication auth) {
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) return null;
        return customerRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    private void populateCommonModel(Model model, Authentication auth, Customer me, String active) {
        model.addAttribute("me", me);
        model.addAttribute("user", me);
        model.addAttribute("active", active);
        model.addAttribute("authm", extractAuthMethod(auth));
    }

    private String extractAuthMethod(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof java.util.Map<?,?> map) {
            Object val = map.get("authm");
            if (val != null) return val.toString();
        }
        return "local";
    }

    private String resolveEmail(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof OAuth2User oauthUser) {
            Object emailAttr = oauthUser.getAttributes().get("email");
            if (emailAttr != null) return emailAttr.toString();
        }
        return auth.getName();
    }

    private VoucherView toVoucherView(Discount discount) {
        String desc = extractDescription(discount.getConditionsJson());
        String valueText = discount.getValueType() == DiscountValueType.PERCENT
                ? discount.getValueAmount().stripTrailingZeros().toPlainString() + "%"
                : formatCurrency(discount.getValueAmount());
        String minOrder = discount.getMinOrder() != null && discount.getMinOrder().compareTo(BigDecimal.ZERO) > 0
                ? "Đơn tối thiểu " + formatCurrency(discount.getMinOrder())
                : "Không giới hạn đơn tối thiểu";
        String expiry = discount.getEndAt() != null
                ? discount.getEndAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Không giới hạn";
        return new VoucherView(
                discount.getCode(),
                valueText,
                desc != null ? desc : "Áp dụng cho mọi sản phẩm.",
                minOrder,
                expiry
        );
    }

    private String extractDescription(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.hasNonNull("desc")) {
                return node.get("desc").asText();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String formatCurrency(BigDecimal value) {
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return fmt.format(value != null ? value : BigDecimal.ZERO);
    }

    private record VoucherView(
            String code,
            String valueText,
            String description,
            String minOrderText,
            String expiryText
    ) {}
}
