package xmate.com.service.catalog;

import xmate.com.entity.catalog.ProductMedia;
import org.springframework.data.domain.*;

import java.util.List;

public interface ProductMediaService {
    ProductMedia create(ProductMedia m);
    ProductMedia update(Long id, ProductMedia m);
    void delete(Long id);
    ProductMedia get(Long id);

    Page<ProductMedia> list(Pageable pageable);
    Page<ProductMedia> search(String q, Pageable pageable);

    List<ProductMedia> forProduct(Long productId);
    List<ProductMedia> forVariant(Long variantId);
}
