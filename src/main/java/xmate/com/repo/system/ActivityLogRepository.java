// src/main/java/xmate/com/repo/system/ActivityLogRepository.java
package xmate.com.repo.system;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.domain.system.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("""
        SELECT a FROM ActivityLog a
        WHERE (:entityType IS NULL OR a.entityType = :entityType)
          AND (:entityId IS NULL OR a.entityId = :entityId)
        """)
    Page<ActivityLog> findByEntity(@Param("entityType") String entityType,
                                   @Param("entityId") Long entityId,
                                   Pageable pageable);

    @Query("""
        SELECT a FROM ActivityLog a
        WHERE (:q IS NULL OR :q = '' 
            OR LOWER(COALESCE(a.entityType,'')) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(COALESCE(a.action,'')) LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<ActivityLog> search(@Param("q") String q, Pageable pageable);

    @Query(value = """
    SELECT COUNT(*)
    FROM activity_logs a
    WHERE DATE(a.created_at) = CURRENT_DATE()
    """, nativeQuery = true)
    long countToday();
}
