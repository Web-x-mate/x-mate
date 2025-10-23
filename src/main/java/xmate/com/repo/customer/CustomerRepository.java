package xmate.com.repo.customer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.customer.CustomerMembership;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    boolean existsByEmail(String email);
    // UserRepository
    boolean existsByEmailIgnoreCase(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmailIgnoreCase(String email);
    Optional<Customer> findByOauthProviderAndOauthSubject(String provider, String subject);


    Page<Customer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByEnabled(Boolean aTrue);

    Page<Customer> findByEmailContainingIgnoreCaseOrFullnameContainingIgnoreCaseOrderByCreatedAtDesc(
            String email, String fullname, Pageable pageable);
}
