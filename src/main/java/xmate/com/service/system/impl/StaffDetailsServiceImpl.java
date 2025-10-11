package xmate.com.service.system.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.system.Permission;
import xmate.com.entity.system.Role;
import xmate.com.entity.system.User; // Nhân viên (staff) của bạn
import xmate.com.repo.system.UserRepository;
import xmate.com.service.system.StaffDetailsService;

import java.util.HashSet;
import java.util.Set;

/**
 * Triển khai StaffDetailsService:
 * - Load User (nhân viên) kèm roles & permissions
 * - Map role -> "ROLE_{name}"
 * - Map permission -> permission_key (vd: "catalog.product.view")
 */
@Slf4j
@Service("staffDetailsService")
@RequiredArgsConstructor
public class StaffDetailsServiceImpl implements StaffDetailsService {

    private final UserRepository userRepo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Lưu ý: method repo này nên fetch join roles -> permissions hoặc dùng @EntityGraph
        User u = userRepo.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        Set<GrantedAuthority> auths = new HashSet<>();

        for (Role r : u.getRoles()) {
            // Vai trò (ROLE_)
            if (r.getName() != null && !r.getName().isBlank()) {
                auths.add(new SimpleGrantedAuthority("ROLE_" + r.getName()));
            }
            // Permissions theo key: module.resource.action
            if (r.getPermissions() != null) {
                for (Permission p : r.getPermissions()) {
                    if (p.getKey() != null && !p.getKey().isBlank()) {
                        auths.add(new SimpleGrantedAuthority(p.getKey()));
                    }
                }
            }
        }

        // Trả về user của Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .authorities(auths)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!u.getActive())
                .build();
    }
}
