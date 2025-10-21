package xmate.com.service.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xmate.com.entity.customer.MembershipTier;

public interface MembershipTierAdminService {
    Page<MembershipTier> list(Pageable pageable);
    MembershipTier get(String code);
    MembershipTier save(MembershipTier tier);
    void delete(String code);
}
