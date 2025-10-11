package xmate.com.repo.catalog;

import xmate.com.entity.catalog.ProductVariant;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {
    Optional<ProductVariant> findBySku(String sku);
    Page<ProductVariant> findAllByProduct_Id(Long productId, Pageable pageable);
}
