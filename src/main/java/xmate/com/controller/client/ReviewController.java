package xmate.com.controller.client;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import xmate.com.controller.client.support.ClientReviewViewService;
import xmate.com.controller.client.view.ReviewItemView;
import xmate.com.entity.catalog.ProductReview;
import xmate.com.entity.common.ReviewStatus;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.sales.Order;
import xmate.com.entity.sales.OrderItem;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.review.ProductReviewRepository;
import xmate.com.repo.sales.OrderItemRepository;

import java.util.List;

@Controller
@RequestMapping("/account/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final CustomerRepository customerRepository;
    private final ClientReviewViewService viewService;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository reviewRepository;

    @GetMapping
    public String list(Authentication auth, Model model) {
        Customer me = requireCustomer(auth);
        List<ReviewItemView> items = viewService.build(me);
        model.addAttribute("me", me);
        model.addAttribute("user", me);
        model.addAttribute("active", "reviews");
        model.addAttribute("reviewItems", items);
        model.addAttribute("pageTitle", "Đánh giá và phản hồi");
        return "client/account/reviews";
    }

    @PostMapping
    public String submit(Authentication auth,
                         @RequestParam("orderItemId") Long orderItemId,
                         @RequestParam("rating") @Min(1) @Max(5) int rating,
                         @RequestParam(value = "content", required = false) String content,
                         RedirectAttributes ra) {
        Customer me = requireCustomer(auth);
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Order order = item.getOrder();
        if (order == null || order.getCustomer() == null || !order.getCustomer().getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() != xmate.com.entity.enums.OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chưa đánh giá đơn đã giao");
        }
        reviewRepository.findByCustomer_IdAndOrderItemId(me.getId(), orderItemId)
                .ifPresent(r -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Chưa gửi đánh giá!"); });

        ProductReview rev = ProductReview.builder()
                .product(item.getProduct())
                .variant(item.getVariant())
                .customer(me)
                .order(order)
                .orderItemId(item.getId())
                .rating(rating)
                .content(content != null ? content.trim() : null)
                .status(ReviewStatus.APPROVED)
                .build();
        reviewRepository.save(rev);
        ra.addFlashAttribute("reviewMessage", "Gửi đánh giá?. Chờ xác nhận.");
        return "redirect:/account/reviews";
    }

    private Customer requireCustomer(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String email = resolveEmail(auth);
        if (email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return customerRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private String resolveEmail(Authentication auth) {
        if (auth.getPrincipal() instanceof org.springframework.security.core.userdetails.User u) {
            return u.getUsername();
        }
        if (auth.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User o) {
            Object e = o.getAttributes().get("email");
            return e != null ? e.toString() : null;
        }
        return null;
    }
}


