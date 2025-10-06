// src/main/java/xmate/com/service/procurement/impl/BatchServiceImpl.java
package xmate.com.service.procurement.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.procurement.Batch;
import xmate.com.repo.procurement.BatchRepository;
import xmate.com.service.procurement.BatchService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchServiceImpl implements BatchService {

    private final BatchRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<Batch> search(Long variantId, Long poItemId, Pageable pageable) {
        return repo.search(variantId, poItemId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> byVariant(Long variantId) {
        return repo.findByVariantId(variantId);
    }

    @Override
    @Transactional(readOnly = true)
    public Batch get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Batch not found: " + id));
    }

    @Override
    public Batch create(Batch b) {
        b.setId(null);
        return repo.save(b);
    }

    @Override
    public Batch update(Long id, Batch b) {
        Batch cur = get(id);
        cur.setVariant(b.getVariant());
        cur.setPurchaseOrderItem(b.getPurchaseOrderItem());
        cur.setUnitCost(b.getUnitCost());
        cur.setQtyReceived(b.getQtyReceived());
        cur.setReceivedAt(b.getReceivedAt());
        return repo.save(cur);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
