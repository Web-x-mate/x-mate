package xmate.com.service.catalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xmate.com.entity.catalog.Product;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

public interface ProductService {
    Product create(Product p);
    Product update(Long id, Product p);
    void delete(Long id);
    Product get(Long id);

    Page<Product> list(Pageable pageable);
    Page<Product> search(String q, Pageable pageable);
    Page<Product> byCategory(Long categoryId, Pageable pageable);
    Optional<Product> findBySlug(String slug);
    Page<Product> byCategories(Collection<Long> categoryIds, Pageable pageable);
    Page<Product> searchInCategories(String q, Collection<Long> categoryIds, Pageable pageable);
    java.util.List<Product> listByCategories(Collection<Long> categoryIds);
    Page<Product> listNewArrivals(LocalDateTime since, Pageable pageable);
    Page<Product> findBySlugKeyword(String slugKeyword, Pageable pageable);
    Page<Product> listDiscounted(double minDiscountRatio, Pageable pageable);
}
