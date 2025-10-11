// src/main/java/xmate/com/repo/procurement/SupplierRepository.java
package xmate.com.repo.procurement;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.procurement.Supplier;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("""
        SELECT s FROM Supplier s
        WHERE (:q IS NULL OR :q = '' 
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(s.email,'')) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(COALESCE(s.phone,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);
}
