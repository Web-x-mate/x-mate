// src/main/java/xmate/com/repo/discount/DiscountRepository.java
package xmate.com.repo.discount;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.discount.Discount;
import xmate.com.entity.common.DiscountKind;

import java.time.LocalDateTime;
import java.util.*;

public interface DiscountRepository extends JpaRepository<Discount, Long>, JpaSpecificationExecutor<Discount> {

    Optional<Discount> findByCodeIgnoreCase(String code);

    @Query("""
      SELECT d FROM Discount d
      WHERE d.status = 'ACTIVE'
        AND (d.startAt IS NULL OR d.startAt <= :now)
        AND (d.endAt   IS NULL OR d.endAt   >= :now)
        AND (d.usageLimit IS NULL OR d.usedCount < d.usageLimit)
        AND d.type = :kind
    """)
    List<Discount> findActiveByKind(@Param("now") LocalDateTime now, @Param("kind") DiscountKind kind);

    @Query("""
      SELECT d FROM Discount d
      WHERE (:q IS NULL OR :q = '' 
             OR LOWER(COALESCE(d.code,'')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(d.conditionsJson,'')) LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Discount> search(@Param("q") String q, Pageable pageable);
}
