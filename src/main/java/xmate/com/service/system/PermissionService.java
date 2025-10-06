// src/main/java/xmate/com/service/system/PermissionService.java
package xmate.com.service.system;

import org.springframework.data.domain.*;
import xmate.com.domain.system.Permission;

public interface PermissionService {
    Page<Permission> search(String q, Pageable pageable);
    Permission get(Long id);
    Permission create(Permission p);
    Permission update(Long id, Permission p);
    void delete(Long id);
}
