// src/main/java/xmate/com/repo/sales/OrderRepository.java
package xmate.com.repo.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.sales.Order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    @Query("""
      SELECT o FROM Order o
      LEFT JOIN o.customer c
      WHERE (:q IS NULL OR :q='' OR
            LOWER(o.code) LIKE LOWER(CONCAT('%',:q,'%')) OR
            (c IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%'))))
        AND (:status IS NULL OR CAST(o.status AS string)=:status)
        AND (:payment IS NULL OR CAST(o.paymentStatus AS string)=:payment)
        AND (:shipping IS NULL OR CAST(o.shippingStatus AS string)=:shipping)
      """)
    Page<Order> search(@Param("q") String q,
                       @Param("status") String status,
                       @Param("payment") String payment,
                       @Param("shipping") String shipping,
                       Pageable pageable);

    // ==========================
    // === Dashboard (MySQL) ===
    // ==========================

    /** Doanh thu đã thanh toán (PAID) trong khoảng ngày — cột đúng là orders.total */
    @Query(value = """
        SELECT COALESCE(SUM(o.total), 0)
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND DATE(o.created_at) BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumPaidRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Tổng số đơn trong khoảng ngày */
    @Query(value = """
        SELECT COUNT(*)
        FROM orders o
        WHERE DATE(o.created_at) BETWEEN :from AND :to
        """, nativeQuery = true)
    long countOrdersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Sparkline: doanh thu theo ngày (MySQL 8 dùng recursive CTE thay generate_series) */
    @Query(value = """
        WITH RECURSIVE d AS (
          SELECT :from AS d
          UNION ALL
          SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM d WHERE d < :to
        )
        SELECT COALESCE(SUM(o.total), 0) AS val
        FROM d
        LEFT JOIN orders o
          ON DATE(o.created_at) = d.d
         AND o.payment_status = 'PAID'
        GROUP BY d.d
        ORDER BY d.d
        """, nativeQuery = true)
    List<Double> sparkRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Sparkline: số đơn theo ngày */
    @Query(value = """
        WITH RECURSIVE d AS (
          SELECT :from AS d
          UNION ALL
          SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM d WHERE d < :to
        )
        SELECT COALESCE(COUNT(o.id), 0) AS val
        FROM d
        LEFT JOIN orders o
          ON DATE(o.created_at) = d.d
        GROUP BY d.d
        ORDER BY d.d
        """, nativeQuery = true)
    List<Integer> sparkOrders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Aggregate doanh thu PAID theo D/W/M → (label,value) */
    @Query(value = """
        SELECT
          CASE :gran
            WHEN 'D' THEN DATE_FORMAT(DATE(o.created_at), '%d/%m')
            WHEN 'W' THEN CONCAT(YEAR(o.created_at), '-W', LPAD(WEEK(o.created_at, 1), 2, '0'))
            ELSE DATE_FORMAT(DATE(o.created_at), '%m/%Y')
          END AS label,
          SUM(o.total) AS value
        FROM orders o
        WHERE o.payment_status = 'PAID'
          AND DATE(o.created_at) BETWEEN :from AND :to
        GROUP BY label
        ORDER BY MIN(DATE(o.created_at))
        """, nativeQuery = true)
    List<LvDouble> aggregatePaidRevenue(@Param("from") LocalDate from,
                                        @Param("to") LocalDate to,
                                        @Param("gran") String gran);
    interface LvDouble { String getLabel(); Double getValue(); }

    /** Phễu trạng thái đơn (đếm theo status) — cast COUNT(*) về SIGNED để map Integer nếu muốn */
    @Query(value = """
        SELECT o.status AS label, CAST(COUNT(*) AS SIGNED) AS value
        FROM orders o
        WHERE DATE(o.created_at) BETWEEN :from AND :to
        GROUP BY o.status
        ORDER BY o.status
        """, nativeQuery = true)
    List<LvInt> funnel(@Param("from") LocalDate from, @Param("to") LocalDate to);
    interface LvInt { String getLabel(); Integer getValue(); }

    /** Đơn gần nhất (bảng dưới) — customers.name & orders.total & DATE_FORMAT */
    @Query(value = """
        SELECT o.code AS code,
               COALESCE(c.name, 'Khách lẻ') AS customer,
               o.total AS total,
               o.payment_status AS paymentStatus,
               o.shipping_status AS shippingStatus,
               DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i') AS createdAt
        FROM orders o
        LEFT JOIN customers c ON c.id = o.customer_id
        ORDER BY o.created_at DESC
        LIMIT :n
        """, nativeQuery = true)
    List<RecentOrderRow> recent(@Param("n") int n);
    interface RecentOrderRow {
        String getCode();
        String getCustomer();
        double getTotal();
        String getPaymentStatus();
        String getShippingStatus();
        String getCreatedAt();
    }
}
