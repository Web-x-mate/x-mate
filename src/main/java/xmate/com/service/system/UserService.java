// src/main/java/xmate/com/service/system/UserService.java
package xmate.com.service.system;

import org.springframework.data.domain.*;
import xmate.com.domain.system.User;

import java.util.Set;

public interface UserService {
    Page<User> search(String q, Pageable pageable);
    User get(Long id);
    User create(User u);
    User update(Long id, User u);
    void delete(Long id);

    User setActive(Long id, boolean active);
    User assignRoles(Long userId, Set<Long> roleIds);
}
