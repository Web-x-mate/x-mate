package xmate.com.repo.customer;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    // UserRepository
    boolean existsByEmailIgnoreCase(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmailIgnoreCase(String email);
    Optional<Customer> findByOauthProviderAndOauthSubject(String provider, String subject);

}
