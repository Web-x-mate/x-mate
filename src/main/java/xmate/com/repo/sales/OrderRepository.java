// src/main/java/xmate/com/repo/sales/OrderRepository.java
package xmate.com.repo.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.sales.Order;
import xmate.com.entity.common.OrderStatus;
import xmate.com.entity.common.PaymentStatus;
import xmate.com.entity.common.ShippingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByCode(String code);
    Page<Order> findByCustomer(Customer customer, Pageable pageable);
    Optional<Order> findByCodeAndCustomer(String code, Customer customer);
    List<Order> findTop30ByCustomerOrderByCreatedAtDesc(Customer customer);

    @Query("select o.status from Order o where o.code = :code")
    Optional<OrderStatus> findStatusByCode(@Param("code") String code);

    // ====== Dashboard: KPIs ======
    @Query(value = """
        SELECT COALESCE(SUM(total),0) 
        FROM orders 
        WHERE payment_status = 'PAID'
          AND DATE(created_at) BETWEEN :from AND :to
        """, nativeQuery = true)
    BigDecimal sumPaidRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
        SELECT COUNT(*) 
        FROM orders 
        WHERE DATE(created_at) BETWEEN :from AND :to
        """, nativeQuery = true)
    long countOrdersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ====== Dashboard: Sparklines (theo ngày) ======
    @Query(value = """
        SELECT DATE(created_at) AS label, SUM(total) AS value
        FROM orders
        WHERE payment_status = 'PAID'
          AND DATE(created_at) BETWEEN :from AND :to
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
        """, nativeQuery = true)
    List<LabelValueD> sparkRevenue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = """
        SELECT DATE(created_at) AS label, COUNT(*) AS value
        FROM orders
        WHERE DATE(created_at) BETWEEN :from AND :to
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
        """, nativeQuery = true)
    List<LabelValueI> sparkOrders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ====== Dashboard: Doanh thu theo ngày/tháng/năm ======
    @Query(value = """
        SELECT 
          CASE 
            WHEN :gran = 'DAY'   THEN DATE(created_at)
            WHEN :gran = 'MONTH' THEN DATE_FORMAT(created_at,'%Y-%m')
            WHEN :gran = 'YEAR'  THEN DATE_FORMAT(created_at,'%Y')
          END AS label,
          SUM(total) AS value
        FROM orders
        WHERE payment_status = 'PAID'
          AND DATE(created_at) BETWEEN :from AND :to
        GROUP BY label
        ORDER BY label
        """, nativeQuery = true)
    List<LabelValueD> aggregatePaidRevenue(@Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("gran") String gran);

    // ====== Dashboard: Funnel trạng thái đơn (đếm theo OrderStatus) ======
    @Query(value = """
        SELECT o.status AS label, COUNT(*) AS value
        FROM orders o
        WHERE DATE(o.created_at) BETWEEN :from AND :to
        GROUP BY o.status
        ORDER BY o.status
        """, nativeQuery = true)
    List<LabelValueI> funnel(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // ====== Dashboard: Recent orders ======
    @Query(value = """
        SELECT 
          o.code                           AS code,
          COALESCE(c.fullname, c.email)   AS customer,
          o.total                          AS total,
          o.payment_status                 AS paymentStatus,
          o.shipping_status                AS shippingStatus,
          o.created_at                     AS createdAt
        FROM orders o
        LEFT JOIN customers c ON c.id = o.customer_id
        ORDER BY o.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecentOrderRow> recent(@Param("limit") int limit);

    // ====== Admin search (đã sửa ở bước trước) ======
    @Query("""
        SELECT o FROM Order o
        WHERE (:q IS NULL OR :q = '' 
              OR LOWER(o.code) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(COALESCE(o.shippingAddress,'')) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status   IS NULL OR o.status         = :status)
          AND (:payment  IS NULL OR o.paymentStatus  = :payment)
          AND (:shipping IS NULL OR o.shippingStatus = :shipping)
        """)
    Page<Order> search(@Param("q") String q,
                       @Param("status") OrderStatus status,
                       @Param("payment") PaymentStatus payment,
                       @Param("shipping") ShippingStatus shipping,
                       Pageable pageable);
}
