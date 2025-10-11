// src/main/java/xmate/com/repo/procurement/PurchaseOrderItemRepository.java
package xmate.com.repo.procurement;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.procurement.PurchaseOrderItem;

import java.util.List;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    @Query("SELECT i FROM PurchaseOrderItem i WHERE i.purchaseOrder.id = :poId")
    List<PurchaseOrderItem> findByPurchaseOrderId(@Param("poId") Long poId);

    @Query("SELECT i FROM PurchaseOrderItem i WHERE i.purchaseOrder.id = :poId")
    Page<PurchaseOrderItem> pageByPurchaseOrderId(@Param("poId") Long poId, Pageable pageable);
}
