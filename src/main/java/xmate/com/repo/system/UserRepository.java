// src/main/java/xmate/com/repo/system/UserRepository.java
package xmate.com.repo.system;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import xmate.com.entity.system.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);


    @Query("""
        SELECT u FROM User u
        WHERE (:q IS NULL OR :q = '' 
            OR LOWER(u.username) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(COALESCE(u.fullName,'')) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(COALESCE(u.email,'')) LIKE LOWER(CONCAT('%',:q,'%'))
            OR LOWER(COALESCE(u.phone,'')) LIKE LOWER(CONCAT('%',:q,'%'))
        )
        """)
    Page<User> search(@Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<User> findByEmail(String email);

    @Query("""
         select distinct u from User u
           left join fetch u.roles r
           left join fetch r.permissions
         where u.email = :email
      """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    Optional<User> findByEmailIgnoreCase(String email);

    @Query("""
       select distinct u from User u
       left join fetch u.roles r
       left join fetch r.permissions
       where lower(u.username) = lower(:key) or lower(u.email) = lower(:key)
    """)
    Optional<User> findByLoginWithRoles(@Param("key") String key);

    Optional<User> findByUsername(String username);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

}
