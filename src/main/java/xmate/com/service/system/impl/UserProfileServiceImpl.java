// src/main/java/xmate/com/service/system/impl/UserProfileServiceImpl.java
package xmate.com.service.system.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import xmate.com.entity.system.User;
import xmate.com.repo.system.UserRepository;
import xmate.com.service.system.UserProfileService;

@Service
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepo;

    public UserProfileServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public User getProfileOf(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Override
    public User updateSelf(String username, String fullName, String phone, String email) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // Validate nhanh (server-side)
        if (!StringUtils.hasText(fullName) || fullName.trim().length() < 2) {
            throw new IllegalArgumentException("Họ tên phải ≥ 2 ký tự.");
        }
        if (StringUtils.hasText(phone)) {
            if (!phone.matches("^0\\d{9}$")) {
                throw new IllegalArgumentException("Số điện thoại VN phải 10 chữ số, bắt đầu bằng 0.");
            }
            if (userRepo.existsByPhoneAndIdNot(phone, u.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng.");
            }
        }
        if (StringUtils.hasText(email)) {
            if (email.length() > 120 || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("Email không hợp lệ.");
            }
            if (userRepo.existsByEmailAndIdNot(email, u.getId())) {
                throw new IllegalArgumentException("Email đã được sử dụng.");
            }
        }

        // Cập nhật cho phép
        u.setFullName(fullName.trim());
        u.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        u.setEmail(StringUtils.hasText(email) ? email.trim() : null);

        // Không chạm: username, password, roles, salary, active, createdAt
        return userRepo.save(u);
    }
}
