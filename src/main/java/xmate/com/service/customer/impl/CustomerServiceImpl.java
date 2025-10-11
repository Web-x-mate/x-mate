// src/main/java/xmate/com/service/customer/impl/CustomerServiceImpl.java
package xmate.com.service.customer.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.customer.LoyaltyAccount;
import xmate.com.entity.customer.Segment;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.repo.customer.LoyaltyAccountRepository;
import xmate.com.repo.customer.SegmentRepository;
import xmate.com.service.customer.CustomerService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repo;
    private final LoyaltyAccountRepository loyaltyRepo;
    private final SegmentRepository segmentRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    @Override
    public Customer create(Customer c, Integer points, String tier, Set<Long> segmentIds) {
        // unique email (nếu có)
        if (c.getEmail() != null && !c.getEmail().isBlank() && repo.existsByEmail(c.getEmail()))
            throw new IllegalArgumentException("Email already exists");

        c.setId(null);
        c.setCreatedAt(LocalDateTime.now());

        // segments
        if (segmentIds != null && !segmentIds.isEmpty()) {
            Set<Segment> segs = new HashSet<>(segmentRepo.findAllById(segmentIds));
            c.setSegments(segs);
        }

        Customer saved = repo.save(c);

        // loyalty (OneToOne with same PK)
        LoyaltyAccount acc = LoyaltyAccount.builder()
                .customer(saved)
                .points(points != null ? points : 0)
                .tier(tier != null ? tier : null)
                .updatedAt(LocalDateTime.now())
                .build();
        loyaltyRepo.save(acc);

        return saved;
    }

    @Override
    public Customer update(Long id, Customer c, Integer points, String tier, Set<Long> segmentIds) {
        Customer db = get(id);

        if (c.getEmail() != null && !c.getEmail().isBlank()
                && !c.getEmail().equalsIgnoreCase(db.getEmail())
                && repo.existsByEmail(c.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        db.setName(c.getName());
        db.setEmail(c.getEmail());
        db.setPhone(c.getPhone());
        db.setAddress(c.getAddress());
        db.setCreatedAt(db.getCreatedAt() != null ? db.getCreatedAt() : LocalDateTime.now());

        // segments
        if (segmentIds != null) {
            Set<Segment> segs = new HashSet<>(segmentRepo.findAllById(segmentIds));
            db.setSegments(segs);
        }

        Customer saved = repo.save(db);

        // loyalty
        LoyaltyAccount acc = loyaltyRepo.findById(saved.getId()).orElse(
                LoyaltyAccount.builder().customer(saved).build()
        );
        if (points != null) acc.setPoints(points);
        if (tier != null || (acc.getTier() != null && tier == null)) acc.setTier(tier);
        acc.setUpdatedAt(LocalDateTime.now());
        loyaltyRepo.save(acc);

        return saved;
    }

    @Override
    public void delete(Long id) {
        // LoyaltyAccount có orphanRemoval=true từ Customer side? (Bạn set orphanRemoval ở Customer cho LoyaltyAccount rồi)
        repo.deleteById(id);
    }
}
