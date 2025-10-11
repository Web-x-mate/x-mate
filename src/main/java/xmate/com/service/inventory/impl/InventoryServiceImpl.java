// src/main/java/xmate/com/service/inventory/impl/InventoryServiceImpl.java
package xmate.com.service.inventory.impl;

import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.catalog.ProductVariant;
import xmate.com.entity.common.InventoryMovementType;
import xmate.com.entity.common.InventoryRefType;
import xmate.com.entity.inventory.Inventory;
import xmate.com.entity.inventory.InventoryMovement;
import xmate.com.entity.system.User;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.repo.inventory.InventoryMovementRepository;
import xmate.com.repo.inventory.InventoryRepository;
import xmate.com.service.inventory.InventoryService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository invRepo;
    private final InventoryMovementRepository movRepo;
    private final ProductVariantRepository variantRepo;
    @PersistenceContext private EntityManager em;

    @Override
    public Page<Inventory> searchStock(String q, Pageable pageable) {
        return invRepo.search(q, pageable);
    }

    @Override
    @Transactional
    public Inventory getOrCreate(Long variantId) {
        return invRepo.findById(variantId).orElseGet(() -> {
            ProductVariant v = variantRepo.findById(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Variant not found: " + variantId));
            Inventory i = new Inventory();
            i.setVariantId(variantId);
            i.setVariant(v);
            i.setQtyOnHand(0);
            i.setQtyReserved(0);
            i.setUpdatedAt(LocalDateTime.now());
            return invRepo.save(i);
        });
    }

    /** Khóa ghi nhẹ để tránh đua dữ liệu (optional, tuỳ DB hỗ trợ) */
    private Inventory lockInventory(Long variantId) {
        Inventory i = invRepo.findById(variantId).orElse(null);
        if (i == null) i = getOrCreate(variantId);
        em.lock(i, LockModeType.PESSIMISTIC_WRITE);
        return i;
    }

    private void appendMovement(ProductVariant variant, int qty,
                                InventoryMovementType type,
                                InventoryRefType refType, Long refId,
                                String note, Long userId) {
        InventoryMovement m = new InventoryMovement();
        m.setVariant(variant);
        m.setQty(qty);
        m.setType(type);
        m.setRefType(refType);
        m.setRefId(refId);
        m.setNote(note);
        if (userId != null) {
            User u = new User(); u.setId(userId);
            m.setCreatedBy(u);
        }
        m.setCreatedAt(LocalDateTime.now());
        movRepo.save(m);
    }

    @Override
    @Transactional
    public Inventory adjustOnHand(Long variantId, int delta,
                                  InventoryRefType refType, Long refId,
                                  String note, Long userId) {
        if (delta == 0) return getOrCreate(variantId);
        Inventory i = lockInventory(variantId);
        int newOnHand = i.getQtyOnHand() + delta;
        if (newOnHand < 0) throw new IllegalArgumentException("Không đủ tồn kho để điều chỉnh: hiện " + i.getQtyOnHand());
        i.setQtyOnHand(newOnHand);
        i.setUpdatedAt(LocalDateTime.now());
        invRepo.save(i);

        ProductVariant v = i.getVariant();
        appendMovement(v, delta,
                delta > 0 ? InventoryMovementType.IN : InventoryMovementType.OUT,
                refType, refId, note, userId);
        return i;
    }

    @Override
    @Transactional
    public Inventory reserve(Long variantId, int qty,
                             InventoryRefType refType, Long refId,
                             String note, Long userId) {
        if (qty <= 0) throw new IllegalArgumentException("Số lượng giữ phải > 0");
        Inventory i = lockInventory(variantId);
        int available = i.getQtyOnHand() - i.getQtyReserved();
        if (available < qty) throw new IllegalArgumentException("Không đủ tồn khả dụng. Available=" + available);
        i.setQtyReserved(i.getQtyReserved() + qty);
        i.setUpdatedAt(LocalDateTime.now());
        invRepo.save(i);

        appendMovement(i.getVariant(), qty, InventoryMovementType.RESERVE, refType, refId, note, userId);
        return i;
    }

    @Override
    @Transactional
    public Inventory release(Long variantId, int qty,
                             InventoryRefType refType, Long refId,
                             String note, Long userId) {
        if (qty <= 0) throw new IllegalArgumentException("Số lượng trả giữ phải > 0");
        Inventory i = lockInventory(variantId);
        if (i.getQtyReserved() < qty) throw new IllegalArgumentException("Reserved không đủ để trả. Reserved=" + i.getQtyReserved());
        i.setQtyReserved(i.getQtyReserved() - qty);
        i.setUpdatedAt(LocalDateTime.now());
        invRepo.save(i);

        appendMovement(i.getVariant(), qty, InventoryMovementType.RELEASE, refType, refId, note, userId);
        return i;
    }

    @Override
    @Transactional
    public Inventory consumeReserved(Long variantId, int qty,
                                     InventoryRefType refType, Long refId,
                                     String note, Long userId) {
        if (qty <= 0) throw new IllegalArgumentException("Số lượng xuất kho phải > 0");
        Inventory i = lockInventory(variantId);
        if (i.getQtyReserved() < qty) throw new IllegalArgumentException("Reserved không đủ để xuất. Reserved=" + i.getQtyReserved());
        if (i.getQtyOnHand() < qty) throw new IllegalArgumentException("On-hand không đủ để xuất. OnHand=" + i.getQtyOnHand());

        i.setQtyReserved(i.getQtyReserved() - qty);
        i.setQtyOnHand(i.getQtyOnHand() - qty);
        i.setUpdatedAt(LocalDateTime.now());
        invRepo.save(i);

        appendMovement(i.getVariant(), qty, InventoryMovementType.COMMIT, refType, refId, note, userId);
        return i;
    }

    @Override
    public Page<InventoryMovement> movements(Long variantId, String q, Pageable pageable) {
        return movRepo.search(variantId, q, pageable);
    }
}
