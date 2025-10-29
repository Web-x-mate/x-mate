package xmate.com.repo.customer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByCustomerId(Long customerId);
    Optional<Address> findByIdAndCustomerId(Long id, Long customerId);
}
