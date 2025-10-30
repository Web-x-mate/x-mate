package xmate.com.service.catalog;


import xmate.com.entity.catalog.ProductVariant;
import org.springframework.data.domain.*;

import java.util.List;

public interface ProductVariantService {
    ProductVariant create(ProductVariant v);
    ProductVariant update(Long id, ProductVariant v);
    void delete(Long id);
    ProductVariant get(Long id);

    Page<ProductVariant> list(Pageable pageable);
    Page<ProductVariant> search(String q, Pageable pageable);
    Page<ProductVariant> byProduct(Long productId, Pageable pageable);
    List<ProductVariant> findByProductId(Long productId);}
