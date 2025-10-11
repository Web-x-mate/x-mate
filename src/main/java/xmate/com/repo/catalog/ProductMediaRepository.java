package xmate.com.repo.catalog;


import xmate.com.entity.catalog.ProductMedia;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long>, JpaSpecificationExecutor<ProductMedia> {
    List<ProductMedia> findAllByProduct_IdOrderBySortOrderAsc(Long productId);
    List<ProductMedia> findAllByVariant_IdOrderBySortOrderAsc(Long variantId);
}
