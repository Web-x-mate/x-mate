// src/main/java/xmate/com/service/procurement/impl/PurchaseOrderServiceImpl.java
package xmate.com.service.procurement.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.common.POStatus;
import xmate.com.domain.procurement.PurchaseOrder;
import xmate.com.domain.procurement.PurchaseOrderItem;
import xmate.com.repo.procurement.PurchaseOrderItemRepository;
import xmate.com.repo.procurement.PurchaseOrderRepository;
import xmate.com.service.procurement.PurchaseOrderService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository itemRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrder> search(String q, POStatus status, Long supplierId, Pageable pageable) {
        return poRepo.search(q, status, supplierId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder get(Long id) {
        return poRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("PO not found: " + id));
    }

    @Override
    public PurchaseOrder create(PurchaseOrder po, List<PurchaseOrderItem> items) {
        po.setId(null);
        PurchaseOrder saved = poRepo.save(po);
        if (items != null) {
            for (PurchaseOrderItem it : items) {
                it.setId(null);
                it.setPurchaseOrder(saved);
            }
            itemRepo.saveAll(items);
        }
        return saved;
    }

    @Override
    public PurchaseOrder update(Long id, PurchaseOrder po, List<PurchaseOrderItem> items) {
        PurchaseOrder cur = get(id);
        cur.setCode(po.getCode());
        cur.setSupplier(po.getSupplier());
        cur.setStatus(po.getStatus());
        cur.setExpectedDate(po.getExpectedDate());
        cur.setUpdatedAt(java.time.LocalDateTime.now());
        PurchaseOrder saved = poRepo.save(cur);

        if (items != null) {
            // Chiến lược đơn giản: xóa hết items cũ rồi thêm mới (dễ kiểm soát)
            List<PurchaseOrderItem> olds = itemRepo.findByPurchaseOrderId(id);
            itemRepo.deleteAll(olds);
            for (PurchaseOrderItem it : items) {
                it.setId(null);
                it.setPurchaseOrder(saved);
            }
            itemRepo.saveAll(items);
        }
        return saved;
    }

    @Override
    public void delete(Long id) {
        // xóa items trước do FK
        List<PurchaseOrderItem> olds = itemRepo.findByPurchaseOrderId(id);
        itemRepo.deleteAll(olds);
        poRepo.deleteById(id);
    }

    @Override
    public PurchaseOrderItem addItem(Long poId, PurchaseOrderItem item) {
        PurchaseOrder po = get(poId);
        item.setId(null);
        item.setPurchaseOrder(po);
        return itemRepo.save(item);
    }

    @Override
    public PurchaseOrderItem updateItem(Long itemId, PurchaseOrderItem item) {
        PurchaseOrderItem cur = itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("PO item not found: " + itemId));
        cur.setVariant(item.getVariant());
        cur.setQty(item.getQty());
        cur.setCost(item.getCost());
        cur.setReceivedQty(item.getReceivedQty() != null ? item.getReceivedQty() : cur.getReceivedQty());
        return itemRepo.save(cur);
    }

    @Override
    public void removeItem(Long itemId) {
        itemRepo.deleteById(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderItem> itemsOf(Long poId) {
        return itemRepo.findByPurchaseOrderId(poId);
    }

    @Override
    public PurchaseOrder changeStatus(Long poId, POStatus status) {
        PurchaseOrder po = get(poId);
        po.setStatus(status);
        po.setUpdatedAt(java.time.LocalDateTime.now());
        return poRepo.save(po);
    }
}
