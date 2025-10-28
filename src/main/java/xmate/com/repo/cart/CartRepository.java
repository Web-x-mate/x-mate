// src/main/java/repository/CartRepository.java
package xmate.com.repo.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.cart.Cart;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Lấy cart theo user.id (đúng cú pháp Spring Data: user -> id)
    Optional<Cart> findByUser_Id(Long userId);

    // Hoặc nếu bạn muốn chắc chắn lấy "một" cart duy nhất:
    Optional<Cart> findFirstByUser_IdOrderByIdAsc(Long userId);

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findByGuestKey(String guestKey);
}
