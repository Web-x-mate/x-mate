// src/main/java/xmate/com/service/procurement/impl/PurchaseOrderItemServiceImpl.java
package xmate.com.service.procurement.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.procurement.PurchaseOrderItem;
import xmate.com.repo.procurement.PurchaseOrderItemRepository;
import xmate.com.service.procurement.PurchaseOrderItemService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderItemServiceImpl implements PurchaseOrderItemService {

    private final PurchaseOrderItemRepository repo;

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderItem get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("PO item not found: " + id));
    }

    @Override
    public PurchaseOrderItem create(PurchaseOrderItem item) {
        item.setId(null);
        return repo.save(item);
    }

    @Override
    public PurchaseOrderItem update(Long id, PurchaseOrderItem item) {
        PurchaseOrderItem cur = get(id);
        cur.setVariant(item.getVariant());
        cur.setQty(item.getQty());
        cur.setCost(item.getCost());
        cur.setReceivedQty(item.getReceivedQty() != null ? item.getReceivedQty() : cur.getReceivedQty());
        return repo.save(cur);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderItem> byPurchaseOrder(Long poId) {
        return repo.findByPurchaseOrderId(poId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderItem> pageByPurchaseOrder(Long poId, Pageable pageable) {
        return repo.pageByPurchaseOrderId(poId, pageable);
    }
}
