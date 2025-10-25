// src/main/java/xmate/com/service/impl/CategoryServiceImpl.java
package xmate.com.service.catalog.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.catalog.Category;
import xmate.com.repo.catalog.CategoryRepository;
import xmate.com.service.catalog.CategoryService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repo;

    @Override
    public Category create(Category c) {
        return repo.save(c);
    }

    @Override
    public Category update(Long id, Category c) {
        Category old = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category not found"));
        old.setName(c.getName());
        old.setSlug(c.getSlug());
        old.setParent(c.getParent());
        old.setActive(c.getActive());
        return repo.save(old);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Category get(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Category not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Category> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Category> search(String q, Pageable pageable) {
        return repo.findAll(categorySpec(q), pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Category> findBySlug(String slug) {
        if (slug == null || slug.isBlank()) return Optional.empty();
        return repo.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Category> findChildren(Long parentId) {
        if (parentId == null) return List.of();
        return repo.findByParent_Id(parentId);
    }

    private Specification<Category> categorySpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase().trim() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("slug")), like)
            );
        };
    }
}
