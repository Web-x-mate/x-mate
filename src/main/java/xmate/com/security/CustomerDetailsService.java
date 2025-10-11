package xmate.com.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import xmate.com.repo.customer.CustomerRepository;

import java.util.Collections;

@Service
public class CustomerDetailsService implements UserDetailsService {
    private final CustomerRepository userRepo;
    public CustomerDetailsService(CustomerRepository userRepo){ this.userRepo = userRepo; }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String key = email == null ? null : email.trim().toLowerCase();
        var u = userRepo.findByEmailIgnoreCase(key)
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));

        // KHÔNG dùng role cho khách hàng -> authorities rỗng
        return User
                .withUsername(u.getEmail())
                .password(u.getPasswordHash())           // BCrypt hash
                .authorities(Collections.emptyList())    // <= quan trọng
                .disabled(!Boolean.TRUE.equals(u.getEnabled()))
                .build();
    }
}
