package xmate.com.repo.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import xmate.com.entity.customer.MembershipTier;

public interface MembershipTierRepo extends JpaRepository<MembershipTier, String> {
    boolean existsByNameIgnoreCase(String name);
}
