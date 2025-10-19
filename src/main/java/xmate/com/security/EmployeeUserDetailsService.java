// src/main/java/xmate/com/security/EmployeeUserDetailsService.java
package xmate.com.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.repo.system.UserRepository;

import java.util.stream.Stream;

@Service // <— Giữ duy nhất bean này (xóa/disable CombinedUserDetailsService)
public class EmployeeUserDetailsService implements UserDetailsService {

    private final UserRepository userRepo;
    public EmployeeUserDetailsService(UserRepository userRepo){ this.userRepo = userRepo; }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String key = login == null ? "" : login.trim();

        var u = userRepo.findByUsernameIgnoreCase(key)
                .or(() -> userRepo.findByEmailIgnoreCase(key))
                .orElseThrow(() -> new UsernameNotFoundException("Staff not found: " + key));

        // ROLE_* + PERMISSION key
        var authorities =
                u.getRoles().stream().flatMap(r -> {
                    var roleAuth = Stream.of(new SimpleGrantedAuthority("ROLE_" + r.getName().toUpperCase()));
                    var permAuth = r.getPermissions().stream()
                            .map(p -> new SimpleGrantedAuthority(p.getKey())); // ví dụ: CATEGORY_VIEW
                    return Stream.concat(roleAuth, permAuth);
                }).toList();

        return User.withUsername(u.getUsername()) // hoặc u.getEmail() tùy bạn hiển thị
                .password(u.getPassword())
                .authorities(authorities)
                .accountLocked(!Boolean.TRUE.equals(u.getActive()))
                .build();
    }
}
