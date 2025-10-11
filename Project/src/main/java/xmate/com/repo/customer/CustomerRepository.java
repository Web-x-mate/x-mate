package xmate.com.repo.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    // UserRepository
    Optional<Customer> findByEmailIgnoreCase(String email);   // thêm dòng này
    boolean existsByEmailIgnoreCase(String email);
    Optional<Customer> findByPhone(String phone);
}
