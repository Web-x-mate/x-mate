package xmate.com.repo.customer;


import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import xmate.com.entity.customer.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    @Query("""
       SELECT c FROM Customer c
       WHERE (:q IS NULL OR :q='' OR
              LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR
              LOWER(c.email) LIKE LOWER(CONCAT('%',:q,'%')) OR
              c.phone LIKE CONCAT('%',:q,'%'))
       """)
    Page<Customer> search(String q, Pageable pageable);
}

