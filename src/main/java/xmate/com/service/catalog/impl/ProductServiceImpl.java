// src/main/java/xmate/com/service/impl/ProductServiceImpl.java
package xmate.com.service.catalog.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.catalog.Product;
import xmate.com.repo.catalog.ProductRepository;
import xmate.com.service.catalog.ProductService;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repo;

    @Override
    public Product create(Product p) {
        // createdAt đã set default trong entity
        return repo.save(p);
    }

    @Override
    public Product update(Long id, Product p) {
        Product old = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        old.setName(p.getName());
        old.setSlug(p.getSlug());
        old.setCategory(p.getCategory());
        old.setStatus(p.getStatus());
        old.setDescription(p.getDescription());
        old.setMaterial(p.getMaterial());
        old.setFit(p.getFit());
        old.setGender(p.getGender());
        old.setUpdatedAt(LocalDateTime.now());
        return repo.save(old);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Product get(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Product> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Product> search(String q, Pageable pageable) {
        return repo.findAll(productSpec(q), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Product> byCategory(Long categoryId, Pageable pageable) {
        return repo.findAllByCategory_Id(categoryId, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Product> byCategories(Collection<Long> categoryIds, Pageable pageable) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return repo.findAllByCategory_IdIn(categoryIds, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Product> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return repo.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Product> searchInCategories(String q, Collection<Long> categoryIds, Pageable pageable) {
        Specification<Product> spec = productSpec(q);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and((root, query, cb) -> root.get("category").get("id").in(categoryIds));
        }
        return repo.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public java.util.List<Product> listByCategories(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return java.util.List.of();
        }
        return repo.findAllByCategory_IdIn(categoryIds);
    }

    private Specification<Product> productSpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase().trim() + "%";

            // join category (left) để search theo tên category nếu muốn
            Join<Object, Object> cat = root.join("category", JoinType.LEFT);

            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("slug")), like),
                    cb.like(cb.lower(root.get("material")), like),
                    cb.like(cb.lower(root.get("fit")), like),
                    cb.like(cb.lower(cat.get("name")), like)
            );
        };
    }
}
