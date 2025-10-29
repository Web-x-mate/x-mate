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

    // Lấy tất cả theo type (không ràng buộc thời gian) – dùng cho /api/coupons
    List<Discount> findAllByType(DiscountKind type);

    @Query("""
        SELECT d FROM Discount d
        WHERE d.status = 'ACTIVE'
          AND (d.startAt IS NULL OR d.startAt <= :now)
          AND (d.endAt   IS NULL OR d.endAt   >= :now)
          AND ( d.usageLimit IS NULL OR COALESCE(d.usedCount,0) < d.usageLimit )
          AND d.type = :kind
    """)
    List<Discount> findActiveByKind(@Param("now") LocalDateTime now,
                                    @Param("kind") DiscountKind kind);

    @Query("""
        SELECT d FROM Discount d
        WHERE d.status = 'ACTIVE'
          AND (d.startAt IS NULL OR d.startAt <= :now)
          AND (d.endAt   IS NULL OR d.endAt   >= :now)
          AND ( d.usageLimit IS NULL OR COALESCE(d.usedCount,0) < d.usageLimit )
          AND d.type IN :kinds
    """)
    List<Discount> findActiveByKinds(@Param("now") LocalDateTime now,
                                     @Param("kinds") List<DiscountKind> kinds);

    @Query("""
      SELECT d FROM Discount d
      WHERE (:q IS NULL OR :q = ''
             OR LOWER(COALESCE(d.code,'')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(d.conditionsJson,'')) LIKE LOWER(CONCAT('%', :q, '%')) )
        AND (:type IS NULL OR :type = '' OR d.type = :type)
        AND (:valueType IS NULL OR :valueType = '' OR d.valueType = :valueType)
    """)
    Page<Discount> search(@Param("q") String q,
                          @Param("type") DiscountKind type,
                          @Param("valueType") DiscountValueType valueType,
                          Pageable pageable);

    @Query(value = "SELECT id FROM discounts WHERE code = :code LIMIT 1", nativeQuery = true)
    Optional<Long> findIdByCode(String code);

    @Modifying
    @Query("""
        update Discount d
           set d.usedCount = coalesce(d.usedCount, 0) + 1
         where d.id = :id
           and d.status = 'ACTIVE'
           and (d.startAt is null or current_timestamp >= d.startAt)
           and (d.endAt   is null or current_timestamp <= d.endAt)
           and (d.usageLimit is null or d.usedCount < d.usageLimit)
    """)
    int incrementUsedCount(@Param("id") Long id);

    // ✅ Giảm used_count (dùng nếu rollback, hủy đơn, hoặc lỗi)
    @Modifying
    @Query("""
        update Discount d
           set d.usedCount = greatest(coalesce(d.usedCount, 0) - 1, 0)
         where d.id = :id
    """)
    int revertUsedCount(@Param("id") Long id);

}
