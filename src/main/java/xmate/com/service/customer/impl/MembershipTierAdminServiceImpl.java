package xmate.com.service.customer.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.customer.MembershipTier;
import xmate.com.repo.customer.MembershipTierRepo;
import xmate.com.service.customer.MembershipTierAdminService;

@Service
@Transactional
public class MembershipTierAdminServiceImpl implements MembershipTierAdminService {

    private final MembershipTierRepo repo;

    public MembershipTierAdminServiceImpl(MembershipTierRepo repo) {
        this.repo = repo;
    }

    @Override @Transactional(readOnly = true)
    public Page<MembershipTier> list(Pageable pageable) {
        return repo.findAll(pageable);
    }

    @Override @Transactional(readOnly = true)
    public MembershipTier get(String code) {
        return repo.findById(code).orElse(null);
    }

    @Override
    public MembershipTier save(MembershipTier tier) {
        return repo.save(tier);
    }

    @Override
    public void delete(String code) {
        repo.deleteById(code);
    }
}
