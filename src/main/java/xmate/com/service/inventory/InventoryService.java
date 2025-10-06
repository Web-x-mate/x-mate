// src/main/java/xmate/com/service/inventory/InventoryService.java
package xmate.com.service.inventory;

import org.springframework.data.domain.*;
import xmate.com.domain.common.InventoryMovementType;
import xmate.com.domain.common.InventoryRefType;
import xmate.com.domain.inventory.Inventory;
import xmate.com.domain.inventory.InventoryMovement;

public interface InventoryService {

    Page<Inventory> searchStock(String q, Pageable pageable);
    Inventory getOrCreate(Long variantId);

    /** Điều chỉnh tồn khả dụng (on hand) theo delta (+/-). Ghi movement. */
    Inventory adjustOnHand(Long variantId, int delta,
                           InventoryRefType refType, Long refId,
                           String note, Long userId);

    /** Giữ hàng (tăng reserved, không đổi on hand). */
    Inventory reserve(Long variantId, int qty,
                      InventoryRefType refType, Long refId,
                      String note, Long userId);

    /** Trả giữ (giảm reserved). */
    Inventory release(Long variantId, int qty,
                      InventoryRefType refType, Long refId,
                      String note, Long userId);

    /** Xuất kho từ lượng đã giữ: giảm reserved và giảm on hand cùng lúc. */
    Inventory consumeReserved(Long variantId, int qty,
                              InventoryRefType refType, Long refId,
                              String note, Long userId);

    Page<InventoryMovement> movements(Long variantId, String q, Pageable pageable);
}
