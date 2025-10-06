package xmate.com.service.catalog;
// src/main/java/xmate/com/service/CategoryService.java

import xmate.com.domain.catalog.Category;
import org.springframework.data.domain.*;

public interface CategoryService {
    Category create(Category c);
    Category update(Long id, Category c);
    void delete(Long id);
    Category get(Long id);

    Page<Category> list(Pageable pageable);
    Page<Category> search(String q, Pageable pageable);
}
