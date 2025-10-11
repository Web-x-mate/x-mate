package xmate.com.util;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import xmate.com.entity.customer.Customer;
import xmate.com.repo.customer.CustomerRepository;

/**
 * Tiện ích lấy thông tin người dùng hiện tại từ Spring Security Context.
 * Dùng được ở mọi nơi: Controller, Service, Template (nếu cần)
 */
@Component
@RequiredArgsConstructor
public final class SecurityUtil {

    private final CustomerRepository userRepo;

    /** 🔹 Lấy email (hoặc username) hiện tại */
    public static String currentUsername() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null) ? null : a.getName();
    }

    /** 🔹 Lấy email người dùng hiện tại (alias cho currentUsername) */
    public static String currentEmail() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (a == null) ? null : a.getName();
    }

    /** 🔹 Lấy ID người dùng hiện tại */
    public Long currentUserId() {
        String email = currentUsername();
        if (email == null) return null;

        Customer u = userRepo.findByEmail(email).orElse(null);
        return (u != null) ? u.getId() : null;
    }

    /** 🔹 Kiểm tra xem người hiện tại có phải ADMIN không */
    public boolean isAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        return a.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
