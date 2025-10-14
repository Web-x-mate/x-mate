package xmate.com.repo.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.cart.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

    void deleteByCartId(Long cartId);

    @Query("SELECT SUM(ci.priceSnap * ci.qty) FROM CartItem ci WHERE ci.cart.user.id = :userId")
    Long sumSubtotalByUserId(@Param("userId") Long userId);

    // --- THÊM CHÚ THÍCH @Query VÀO ĐÂY ---
    // Câu query này sẽ tải tất cả CartItem cùng với Variant và Product liên quan
    // trong 1 lần duy nhất, giúp khắc phục vấn đề load chậm (lỗi N+1).
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.variant v JOIN FETCH v.product p WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartIdWithDetails(@Param("cartId") Long cartId);
}