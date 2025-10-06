// src/main/java/xmate/com/repo/inventory/InventoryMovementRepository.java
package xmate.com.repo.inventory;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.domain.inventory.InventoryMovement;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query("""
      SELECT m FROM InventoryMovement m
        JOIN m.variant v
        LEFT JOIN v.product p
      WHERE (:variantId IS NULL OR v.id = :variantId)
        AND (:q IS NULL OR :q = ''
             OR LOWER(COALESCE(v.sku,'')) LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(COALESCE(p.name,'')) LIKE LOWER(CONCAT('%',:q,'%'))
             OR LOWER(COALESCE(m.note,'')) LIKE LOWER(CONCAT('%',:q,'%')))
    """)
    Page<InventoryMovement> search(@Param("variantId") Long variantId,
                                   @Param("q") String q,
                                   Pageable pageable);
}
