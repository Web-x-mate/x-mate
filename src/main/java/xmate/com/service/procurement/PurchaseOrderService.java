// src/main/java/xmate/com/service/procurement/PurchaseOrderService.java
package xmate.com.service.procurement;

import org.springframework.data.domain.*;
import xmate.com.domain.common.POStatus;
import xmate.com.domain.procurement.PurchaseOrder;
import xmate.com.domain.procurement.PurchaseOrderItem;

import java.util.List;

public interface PurchaseOrderService {
    Page<PurchaseOrder> search(String q, POStatus status, Long supplierId, Pageable pageable);
    PurchaseOrder get(Long id);
    PurchaseOrder create(PurchaseOrder po, List<PurchaseOrderItem> items);
    PurchaseOrder update(Long id, PurchaseOrder po, List<PurchaseOrderItem> items);
    void delete(Long id);

    // thao tác item nhanh
    PurchaseOrderItem addItem(Long poId, PurchaseOrderItem item);
    PurchaseOrderItem updateItem(Long itemId, PurchaseOrderItem item);
    void removeItem(Long itemId);
    List<PurchaseOrderItem> itemsOf(Long poId);

    // đổi trạng thái PO
    PurchaseOrder changeStatus(Long poId, POStatus status);


}
