// src/main/java/xmate/com/service/impl/ProductVariantServiceImpl.java
package xmate.com.service.catalog.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.catalog.ProductVariant;
import xmate.com.repo.catalog.ProductVariantRepository;
import xmate.com.service.catalog.ProductVariantService;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository repo;

    @Override
    public ProductVariant create(ProductVariant v) {
        return repo.save(v);
    }

    @Override
    public ProductVariant update(Long id, ProductVariant v) {
        ProductVariant old = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Variant not found"));
        old.setProduct(v.getProduct());
        old.setSku(v.getSku());
        old.setColor(v.getColor());
        old.setSize(v.getSize());
        old.setBarcode(v.getBarcode());
        old.setPrice(v.getPrice());
        old.setCompareAtPrice(v.getCompareAtPrice());
        old.setCost(v.getCost());
        old.setWeightGram(v.getWeightGram());
        old.setStockPolicy(v.getStockPolicy());
        old.setActive(v.getActive());
        // giữ nguyên createdAt theo entity
        return repo.save(old);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductVariant get(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Variant not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductVariant> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductVariant> search(String q, Pageable pageable) {
        return repo.findAll(variantSpec(q), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductVariant> byProduct(Long productId, Pageable pageable) {
        return repo.findAllByProduct_Id(productId, pageable);
    }

    private Specification<ProductVariant> variantSpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("sku")), like),
                    cb.like(cb.lower(root.get("color")), like),
                    cb.like(cb.lower(root.get("size")), like),
                    cb.like(cb.lower(root.get("barcode")), like)
            );
        };
    }
}
