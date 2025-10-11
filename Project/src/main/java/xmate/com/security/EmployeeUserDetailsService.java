// xmate/com/security/EmployeeUserDetailsService.java
package xmate.com.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import xmate.com.repo.system.UserRepository;

// xmate/com/security/EmployeeUserDetailsService.java
@Service
public class EmployeeUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;
    public EmployeeUserDetailsService(UserRepository userRepo){ this.userRepo = userRepo; }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String key = login == null ? "" : login.trim();

        var u = userRepo.findByLoginWithRoles(key)
                .orElseThrow(() -> new UsernameNotFoundException("Staff not found"));

        var authorities = u.getRoles().stream()
                .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r.getName().toUpperCase()))
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername())
                .password(u.getPassword())
                .authorities(authorities)
                .accountLocked(!Boolean.TRUE.equals(u.getActive()))
                .build();
    }
}
