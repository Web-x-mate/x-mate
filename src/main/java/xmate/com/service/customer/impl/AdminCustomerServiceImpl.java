package xmate.com.service.customer.impl;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.customer.CustomerMembership;
import xmate.com.repo.customer.AddressRepository;
import xmate.com.repo.customer.CustomerMembershipRepo;
import xmate.com.repo.customer.CustomerRepository;
import xmate.com.service.customer.AdminCustomerService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminCustomerServiceImpl implements AdminCustomerService {

    private final CustomerRepository customerRepo;
    private final CustomerMembershipRepo membershipRepo;
    private final AddressRepository addressRepo;

    public AdminCustomerServiceImpl(CustomerRepository customerRepo,
                                    CustomerMembershipRepo membershipRepo,
                                    AddressRepository addressRepo) {
        this.customerRepo = customerRepo;
        this.membershipRepo = membershipRepo;
        this.addressRepo = addressRepo;
    }

    @Override
    public Page<Customer> getNewestCustomers(Pageable pageable) {
        return customerRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Map<Long, CustomerMembership> getMembershipsFor(Page<Customer> customers) {
        List<Long> ids = customers.getContent().stream().map(Customer::getId).toList();
        if (ids.isEmpty()) return Map.of();
        return membershipRepo.findByUserIdIn(ids).stream()
                .collect(Collectors.toMap(CustomerMembership::getUserId, Function.identity()));
    }

    @Override public long countAll()      { return customerRepo.count(); }
    @Override public long countEnabled()  { return customerRepo.countByEnabled(Boolean.TRUE); }
    @Override public long countDisabled() { return customerRepo.countByEnabled(Boolean.FALSE); }

    @Override
    public Page<Customer> search(String q, Pageable pageable) {
        if (q == null || q.isBlank())
            return customerRepo.findAllByOrderByCreatedAtDesc(pageable);
        return customerRepo
                .findByEmailContainingIgnoreCaseOrFullnameContainingIgnoreCaseOrderByCreatedAtDesc(q, q, pageable);
    }

    @Override
    public Customer get(Long id) {
        return customerRepo.findById(id).orElse(null);
    }

    // ⚠️ Ghi đè readOnly=false cho method ghi
    @Override
    @Transactional
    public Customer save(Customer c) {
        // entity tự normalize ở @PrePersist/@PreUpdate
        return customerRepo.saveAndFlush(c);
    }

    // ⚠️ Ghi đè readOnly=false + xóa con trước cha để tránh FK RESTRICT
    @Override
    @Transactional
    public void delete(Long id) {
        customerRepo.deleteById(id);
        customerRepo.flush(); // nổ lỗi ngay nếu còn FK
    }
}
