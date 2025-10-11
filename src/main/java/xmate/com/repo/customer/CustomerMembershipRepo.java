package xmate.com.repo.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.CustomerMembership;

public interface CustomerMembershipRepo extends JpaRepository<CustomerMembership, Long> {}
