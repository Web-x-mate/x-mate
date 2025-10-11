package xmate.com.service.catalog;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;


import xmate.com.entity.catalog.Product;

public interface ProductService {
    Product create(Product p);
    Product update(Long id, Product p);
    void delete(Long id);
    Product get(Long id);

    Page<Product> list(Pageable pageable);
    Page<Product> search(String q, Pageable pageable);
    Page<Product> byCategory(Long categoryId, Pageable pageable);
}
