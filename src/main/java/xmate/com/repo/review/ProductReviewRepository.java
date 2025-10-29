package xmate.com.repo.review;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.catalog.ProductReview;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByCustomer_IdAndOrderItemId(Long customerId, Long orderItemId);
    List<ProductReview> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
}

