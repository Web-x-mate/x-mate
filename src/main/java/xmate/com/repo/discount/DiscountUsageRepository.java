package xmate.com.repo.discount;


import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.discount.*;

import java.time.LocalDate;
import java.util.List;
public interface DiscountUsageRepository extends JpaRepository<DiscountUsage, DiscountUsageId> {

    @Query("SELECT COUNT(u) FROM DiscountUsage u WHERE u.discount.id = :discountId")
    long countByDiscountId(@Param("discountId") Long discountId);

    @Query("SELECT COUNT(u) FROM DiscountUsage u WHERE u.discount.id = :discountId AND u.customer.id = :customerId")
    long countByDiscountIdAndCustomerId(@Param("discountId") Long discountId, @Param("customerId") Long customerId);

    boolean existsById(DiscountUsageId id);

    @Query(value = """
    SELECT COUNT(DISTINCT du.order_id)
    FROM discount_usages du
    JOIN orders o ON o.id = du.order_id
    WHERE DATE(o.created_at) BETWEEN :from AND :to
    """, nativeQuery = true)
    long countOrdersUsedDiscount(@Param("from") java.time.LocalDate from,
                                 @Param("to")   java.time.LocalDate to);



    /** Hiệu quả theo mã (Top N) */
    @Query(value = """
        SELECT
          COALESCE(d.code, CONCAT('AUTO#', d.id)) AS label,
          COUNT(u.order_id)                       AS orders,
          COALESCE(SUM(o.total),0)                AS revenue
        FROM discount_usages u
        JOIN discounts d ON d.id = u.discount_id
        JOIN orders o    ON o.id = u.order_id
        WHERE DATE(u.used_at) BETWEEN :from AND :to
        GROUP BY d.id, d.code
        ORDER BY revenue DESC

        """, nativeQuery = true)
    List<EffectRow> effectiveness(@Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("top") int top);

    interface EffectRow {
        String getLabel();
        Integer getOrders();
        Double getRevenue();
    }

    @Query(value = "select count(*) from discount_usages where order_id = :orderId", nativeQuery = true)
    int countByOrderId(Long orderId);

    @Modifying
    @Transactional
    @Query(value = "insert into discount_usages (used_at, customer_id, discount_id, order_id) " +
            "values (CURRENT_TIMESTAMP, :customerId, :discountId, :orderId)", nativeQuery = true)
    void insertUsage(Long customerId, Long discountId, Long orderId);


}
