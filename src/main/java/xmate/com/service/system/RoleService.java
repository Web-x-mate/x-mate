// src/main/java/xmate/com/service/system/RoleService.java
package xmate.com.service.system;

import org.springframework.data.domain.*;
import xmate.com.entity.system.Role;

import java.util.Set;

public interface RoleService {
    Page<Role> search(String q, Pageable pageable);
    Role get(Long id);
    Role create(Role r);
    Role update(Long id, Role r);
    void delete(Long id);

    Role assignPermissions(Long roleId, Set<Long> permIds);
}
