package xmate.com.service.catalog;
// src/main/java/xmate/com/service/CategoryService.java

import xmate.com.entity.catalog.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    Category create(Category c);
    Category update(Long id, Category c);
    void delete(Long id);
    Category get(Long id);

    Page<Category> list(Pageable pageable);
    Page<Category> search(String q, Pageable pageable);
    Optional<Category> findBySlug(String slug);
    List<Category> findChildren(Long parentId);
}
