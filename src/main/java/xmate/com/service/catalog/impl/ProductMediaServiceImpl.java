// src/main/java/xmate/com/service/impl/ProductMediaServiceImpl.java
package xmate.com.service.catalog.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.catalog.ProductMedia;
import xmate.com.repo.catalog.ProductMediaRepository;
import xmate.com.service.catalog.ProductMediaService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductMediaServiceImpl implements ProductMediaService {

    private final ProductMediaRepository repo;

    @Override
    public ProductMedia create(ProductMedia m) {
        return repo.save(m);
    }

    @Override
    public ProductMedia update(Long id, ProductMedia m) {
        ProductMedia old = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Media not found"));
        old.setProduct(m.getProduct());
        old.setVariant(m.getVariant());
        old.setMediaType(m.getMediaType());
        old.setUrl(m.getUrl());
        old.setPrimary(m.getPrimary());
        old.setSortOrder(m.getSortOrder());
        return repo.save(old);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public ProductMedia get(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Media not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductMedia> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ProductMedia> search(String q, Pageable pageable) {
        return repo.findAll(mediaSpec(q), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductMedia> forProduct(Long productId) {
        return repo.findAllByProduct_IdOrderBySortOrderAsc(productId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProductMedia> forVariant(Long variantId) {
        return repo.findAllByVariant_IdOrderBySortOrderAsc(variantId);
    }

    private Specification<ProductMedia> mediaSpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase().trim() + "%";
            return cb.like(cb.lower(root.get("url")), like);
        };
    }
}
