package xmate.com.repo.sales;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import xmate.com.entity.sales.OrderItem;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
    void deleteByOrderId(Long orderId);

    // === Dashboard: Top sản phẩm/biến thể theo số lượng (MySQL) ===
    @Query(value = """
        SELECT
          CONCAT(
            p.name,
            IFNULL(CONCAT(' ', pv.color), ''),
            IFNULL(CONCAT(' ', pv.size), ''),
            ' (', pv.sku, ')'
          ) AS name,
          SUM(oi.qty) AS qty
        FROM order_items oi
        JOIN orders o           ON o.id  = oi.order_id
        JOIN product_variants pv ON pv.id = oi.variant_id
        JOIN products p          ON p.id  = pv.product_id
        WHERE DATE(o.created_at) BETWEEN :from AND :to
        GROUP BY p.name, pv.color, pv.size, pv.sku
        ORDER BY SUM(oi.qty) DESC
        """, nativeQuery = true)
    List<TopVariantRow> topVariantsByQty(@Param("from") LocalDate from,
                                         @Param("to") LocalDate to,
                                         @Param("top") int top);

    interface TopVariantRow {
        String getName();
        Integer getQty();
    }

    // === Dashboard: Top products (aggregate variants) ===
    @Query(value = """
        SELECT 
          p.id           AS productId,
          p.name         AS productName,
          SUM(oi.qty)    AS qty
        FROM order_items oi
        JOIN orders o            ON o.id  = oi.order_id
        JOIN product_variants pv ON pv.id = oi.variant_id
        JOIN products p          ON p.id  = pv.product_id
        WHERE DATE(o.created_at) BETWEEN :from AND :to
        GROUP BY p.id, p.name
        ORDER BY SUM(oi.qty) DESC
        """, nativeQuery = true)
    List<TopProductRow> topProductsByQty(@Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    interface TopProductRow {
        Long getProductId();
        String getProductName();
        Integer getQty();
    }
}
