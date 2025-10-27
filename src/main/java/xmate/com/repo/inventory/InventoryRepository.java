// src/main/java/xmate/com/repo/inventory/InventoryRepository.java
package xmate.com.repo.inventory;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.inventory.Inventory;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

//    @Query("""
//      SELECT i FROM Inventory i
//        JOIN i.variant v
//        LEFT JOIN v.product p
//      WHERE (1=1) AND (:q IS NULL OR :q = ''
//             OR LOWER(COALESCE(v.sku,'')) LIKE LOWER(CONCAT('%',:q,'%'))
//             OR LOWER(COALESCE(v.color,'')) LIKE LOWER(CONCAT('%',:q,'%'))
//             OR LOWER(COALESCE(v.size,''))  LIKE LOWER(CONCAT('%',:q,'%'))
//             OR LOWER(COALESCE(p.name,''))  LIKE LOWER(CONCAT('%',:q,'%')))
//    """)
            @Query("""
          SELECT i FROM Inventory i
            LEFT JOIN FETCH i.variant v  
            LEFT JOIN FETCH v.product p
          WHERE 
            (1=1) AND 
            (:q IS NULL OR :q = ''
                 OR LOWER(COALESCE(v.sku,'')) LIKE LOWER(CONCAT('%',:q,'%'))
                 OR LOWER(COALESCE(v.color,'')) LIKE LOWER(CONCAT('%',:q,'%'))
                 OR LOWER(COALESCE(v.size,''))  LIKE LOWER(CONCAT('%',:q,'%'))
                 OR LOWER(COALESCE(p.name,''))  LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<Inventory> search(@Param("q") String q, Pageable pageable);

    List<Inventory> findByVariantIdIn(List<Long> variantIds);
    @Query(value = """
    SELECT COUNT(*)
    FROM inventory i
    WHERE i.qty_on_hand < :threshold
    """, nativeQuery = true)
    long countLowStock(@Param("threshold") int threshold);

    @Query(value = """
        SELECT
          pv.sku AS sku,
          p.name AS name,
          i.qty_on_hand AS onHand,
          i.qty_reserved AS reserved
        FROM inventory i
        JOIN product_variants pv ON pv.id = i.variant_id
        JOIN products p ON p.id = pv.product_id
        WHERE i.qty_on_hand < :threshold
        ORDER BY i.qty_on_hand ASC, pv.sku ASC
        """, nativeQuery = true)
    List<LowSkuRow> findLowStock(@Param("threshold") int threshold, @Param("top") int top);

    interface LowSkuRow {
        String getSku();
        String getName();
        Integer getOnHand();
        Integer getReserved();
    }


    @Query(value = """
        SELECT
          CONCAT(p.name, ' (', pv.sku, ')') AS label,
          i.qty_on_hand AS onHand,
          i.qty_reserved AS reserved
        FROM inventory i
        JOIN product_variants pv ON pv.id = i.variant_id
        JOIN products p          ON p.id  = pv.product_id
        ORDER BY (i.qty_on_hand + i.qty_reserved) DESC
        """, nativeQuery = true)
    List<StockRow> topStock(@Param("top") int top);

    interface StockRow {
        String getLabel();
        Integer getOnHand();
        Integer getReserved();
    }
}
