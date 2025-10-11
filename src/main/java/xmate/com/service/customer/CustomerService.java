package xmate.com.service.customer;

import org.springframework.data.domain.*;
import xmate.com.entity.customer.Customer;

import java.util.Set;

public interface CustomerService {
    Page<Customer> search(String q, Pageable pageable);
    Customer get(Long id);
    Customer create(Customer c, Integer points, String tier, Set<Long> segmentIds);
    Customer update(Long id, Customer c, Integer points, String tier, Set<Long> segmentIds);
    void delete(Long id);
}
