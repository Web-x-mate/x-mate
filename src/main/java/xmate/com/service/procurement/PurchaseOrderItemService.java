// src/main/java/xmate/com/service/procurement/PurchaseOrderItemService.java
package xmate.com.service.procurement;

import org.springframework.data.domain.*;
import xmate.com.domain.procurement.PurchaseOrderItem;

import java.util.List;

public interface PurchaseOrderItemService {
    PurchaseOrderItem get(Long id);
    PurchaseOrderItem create(PurchaseOrderItem item);
    PurchaseOrderItem update(Long id, PurchaseOrderItem item);
    void delete(Long id);

    List<PurchaseOrderItem> byPurchaseOrder(Long poId);
    Page<PurchaseOrderItem> pageByPurchaseOrder(Long poId, Pageable pageable);
}
