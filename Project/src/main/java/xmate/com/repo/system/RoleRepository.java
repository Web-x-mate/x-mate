// src/main/java/xmate/com/repo/system/RoleRepository.java
package xmate.com.repo.system;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.system.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    @Query("""
        SELECT r FROM Role r
        WHERE (:q IS NULL OR :q = '' 
            OR LOWER(r.name) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(COALESCE(r.description,'')) LIKE LOWER(CONCAT('%',:q,'%')))
        """)
    Page<Role> search(@Param("q") String q, Pageable pageable);
}
