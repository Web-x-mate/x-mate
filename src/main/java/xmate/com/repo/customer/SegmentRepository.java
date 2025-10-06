package xmate.com.repo.customer;


import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.domain.customer.Segment;
import java.util.List;
import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {

    Optional<Segment> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Query("""
       SELECT s FROM Segment s
       WHERE (:q IS NULL OR :q='' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')))
       """)
    Page<Segment> search(@Param("q") String q, Pageable pageable);

    @Query(value = """
        SELECT label, value FROM (
          SELECT s.name AS label, COUNT(cs.customer_id) AS value
          FROM segments s
          LEFT JOIN customer_segments cs ON cs.segment_id = s.id
          GROUP BY s.id, s.name
          UNION ALL
          SELECT 'Unsegmented' AS label, COUNT(*) AS value
          FROM customers c
          LEFT JOIN customer_segments cs ON cs.customer_id = c.id
          WHERE cs.segment_id IS NULL
        ) t
        ORDER BY label
        """, nativeQuery = true)
    List<LvInt> distribution();
    interface LvInt { String getLabel(); Integer getValue(); }
}
