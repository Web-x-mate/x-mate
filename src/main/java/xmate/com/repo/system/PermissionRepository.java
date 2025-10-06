// src/main/java/xmate/com/repo/system/PermissionRepository.java
package xmate.com.repo.system;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.domain.system.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    @Query("""
        SELECT p FROM Permission p
        WHERE (:q IS NULL OR :q = '' 
            OR LOWER(p.key) LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<Permission> search(@Param("q") String q, Pageable pageable);
}
