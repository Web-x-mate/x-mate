package xmate.com.repo.catalog;


import xmate.com.entity.catalog.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findBySlug(String slug);
    Page<Product> findAllByCategory_Id(Long categoryId, Pageable pageable);
}
