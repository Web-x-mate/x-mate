// src/main/java/xmate/com/repo/procurement/BatchRepository.java
package xmate.com.repo.procurement;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.domain.procurement.Batch;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Query("SELECT b FROM Batch b WHERE b.variant.id = :variantId ORDER BY b.receivedAt DESC, b.id DESC")
    List<Batch> findByVariantId(@Param("variantId") Long variantId);

    @Query("""
        SELECT b FROM Batch b 
        WHERE (:variantId IS NULL OR b.variant.id = :variantId)
          AND (:poItemId IS NULL OR b.purchaseOrderItem.id = :poItemId)
        ORDER BY b.receivedAt DESC, b.id DESC
        """)
    Page<Batch> search(@Param("variantId") Long variantId,
                       @Param("poItemId") Long poItemId,
                       Pageable pageable);
}
