package xmate.com.security;
//Map Role/Permission thành GrantedAuthority

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import xmate.com.entity.system.Role;
import xmate.com.entity.system.User;

import java.util.Set;
import java.util.stream.Collectors;

public final class UserToPrincipalMapper {
    private UserToPrincipalMapper(){}

    public static org.springframework.security.core.userdetails.User toSpringUser(User u) {
        Set<SimpleGrantedAuthority> authorities = u.getRoles().stream().flatMap((Role r) -> {
            var roleAuth = java.util.stream.Stream.of(new SimpleGrantedAuthority("ROLE_" + r.getName()));
            var permAuth = r.getPermissions().stream()
                    .map(p -> new SimpleGrantedAuthority(p.getKey()));
            return java.util.stream.Stream.concat(roleAuth, permAuth);
        }).collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(),
                Boolean.TRUE.equals(u.getActive()),
                true, true, true,
                authorities
        );
    }
}
