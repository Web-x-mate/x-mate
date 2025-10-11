// src/main/java/xmate/com/repo/procurement/PurchaseOrderRepository.java
package xmate.com.repo.procurement;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.procurement.PurchaseOrder;
import xmate.com.entity.common.POStatus;

import java.util.List;
import java.time.LocalDate;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    @Query("""
        SELECT p FROM PurchaseOrder p
        WHERE (:q IS NULL OR :q = '' OR LOWER(p.code) LIKE LOWER(CONCAT('%', :q, '%')))
          AND (:status IS NULL OR p.status = :status)
          AND (:supplierId IS NULL OR p.supplier.id = :supplierId)
        """)
    Page<PurchaseOrder> search(@Param("q") String q,
                               @Param("status") POStatus status,
                               @Param("supplierId") Long supplierId,
                               Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM purchase_orders p WHERE p.status IN ('SUBMITTED','PARTIALLY_RECEIVED')",
            nativeQuery = true)
    long countOpenPO();
    // === Dashboard queries (MySQL) ===
    @Query(value = """
        SELECT p.status AS label, COUNT(*) AS value
        FROM purchase_orders p
        GROUP BY p.status
        ORDER BY p.status
        """, nativeQuery = true)
    List<StatusCount> countByStatus();

    @Query(value = """
        SELECT
          p.code AS code,
          s.name AS supplier,
          p.status AS status,
          CAST(COALESCE(SUM(i.received_qty),0) AS SIGNED) AS receivedQty,
          CAST(COALESCE(SUM(i.qty),0) AS SIGNED) AS totalQty
        FROM purchase_orders p
        JOIN suppliers s ON s.id = p.supplier_id
        LEFT JOIN purchase_order_items i ON i.po_id = p.id
        WHERE p.status IN ('SUBMITTED','PARTIALLY_RECEIVED')
        GROUP BY p.id, p.code, s.name, p.status
        ORDER BY p.created_at DESC, p.id DESC
        LIMIT :top
        """, nativeQuery = true)
    List<PoReceivingRow> receiving(@Param("top") int top);

    interface PoReceivingRow {
        String getCode();
        String getSupplier();
        String getStatus();
        Integer getReceivedQty();
        Integer getTotalQty();
    }
    @Query(value = """
        SELECT p.status AS label, COUNT(*) AS value
        FROM purchase_orders p
        WHERE DATE(p.created_at) BETWEEN :from AND :to
        GROUP BY p.status
        ORDER BY p.status
        """, nativeQuery = true)
    List<LvInt> countByStatusBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
    interface LvInt { String getLabel(); Integer getValue(); }





}
