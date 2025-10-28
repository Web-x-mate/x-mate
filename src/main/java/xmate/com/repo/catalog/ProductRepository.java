package xmate.com.repo.catalog;


import xmate.com.entity.catalog.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    Page<Product> findAllByCategory_Id(Long categoryId, Pageable pageable);
    Page<Product> findAllByCategory_IdIn(Collection<Long> categoryIds, Pageable pageable);
    List<Product> findAllByCategory_IdIn(Collection<Long> categoryIds);
    Page<Product> findAllByCreatedAtGreaterThanEqual(java.time.LocalDateTime since, Pageable pageable);
    Page<Product> findAllBySlugContainingIgnoreCase(String slugKeyword, Pageable pageable);

    @Query("""
        select distinct p from Product p
        join ProductVariant v on v.product = p
        where v.compareAtPrice is not null and v.compareAtPrice > 0
          and v.price is not null
          and (v.compareAtPrice - v.price) >= :ratio * v.compareAtPrice
    """)
    Page<Product> findAllWithMinDiscount(@Param("ratio") double ratio, Pageable pageable);
}
