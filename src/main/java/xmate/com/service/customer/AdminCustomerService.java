package xmate.com.service.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import xmate.com.entity.customer.Customer;
import xmate.com.entity.customer.CustomerMembership;

import java.util.Map;

public interface AdminCustomerService {
    Page<Customer> getNewestCustomers(Pageable pageable);

    /** Lấy memberships theo map<customerId, membership> cho 1 trang */
    Map<Long, CustomerMembership> getMembershipsFor(Page<Customer> customers);

    long countAll();
    long countEnabled();
    long countDisabled();

    Page<Customer> search(String q, Pageable pageable);
    Customer get(Long id);
    Customer save(Customer c);
    void delete(Long id);
}
