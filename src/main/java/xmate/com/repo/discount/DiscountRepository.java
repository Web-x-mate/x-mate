package xmate.com.repo.discount;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.common.DiscountKind;
import xmate.com.entity.common.DiscountValueType;
import xmate.com.entity.discount.Discount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    List<Discount> findActiveByKind(@Param("now") LocalDateTime now,
                                    @Param("kind") DiscountKind kind);

    @Query("""
      SELECT d FROM Discount d
      WHERE (:q IS NULL OR :q = ''
             OR LOWER(COALESCE(d.code,'')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(d.conditionsJson,'')) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:type IS NULL OR :type = '' OR d.type = :type)
        AND (:valueType IS NULL OR :valueType = '' OR d.valueType = :valueType)
    """)
    Page<Discount> search(@Param("q") String q,
                          @Param("type") DiscountKind type,
                          @Param("valueType") DiscountValueType valueType,
                          Pageable pageable);

}