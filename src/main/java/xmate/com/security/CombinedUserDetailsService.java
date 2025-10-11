// xmate/com/security/CombinedUserDetailsService.java
package xmate.com.security;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@Primary
public class CombinedUserDetailsService implements UserDetailsService {
    private final CustomerDetailsService customer;
    private final EmployeeUserDetailsService employee;

    public CombinedUserDetailsService(CustomerDetailsService customer,
                                      EmployeeUserDetailsService employee) {
        this.customer = customer;
        this.employee = employee;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        try { return employee.loadUserByUsername(username); }
        catch (UsernameNotFoundException ex) { /* fallthrough */ }
        return customer.loadUserByUsername(username); // ném UsernameNotFound nếu cả 2 đều fail
    }
}

