// src/main/java/xmate/com/service/system/impl/UserServiceImpl.java
package xmate.com.service.system.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.system.Role;
import xmate.com.domain.system.User;
import xmate.com.repo.system.RoleRepository;
import xmate.com.repo.system.UserRepository;
import xmate.com.service.system.UserService;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;

    @Override
    public Page<User> search(String q, Pageable pageable) {
        return userRepo.search(q, pageable);
    }

    @Override
    public User get(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public User create(User u) {
        // tùy bạn băm mật khẩu ở đây nếu cần
        return userRepo.save(u);
    }

    @Override
    public User update(Long id, User u) {
        User db = get(id);
        db.setFullName(u.getFullName());
        db.setEmail(u.getEmail());
        db.setPhone(u.getPhone());
        db.setSalary(u.getSalary());
        db.setActive(u.getActive() != null ? u.getActive() : db.getActive());
        if (u.getPassword()!=null && !u.getPassword().isBlank()) {
            db.setPassword(u.getPassword()); // nếu dùng BCrypt, encode tại đây
        }
        return db;
    }

    @Override
    public void delete(Long id) {
        userRepo.deleteById(id);
    }

    @Override
    public User setActive(Long id, boolean active) {
        User db = get(id);
        db.setActive(active);
        return db;
    }

    @Override
    public User assignRoles(Long userId, Set<Long> roleIds) {
        User db = get(userId);
        Set<Role> roles = new HashSet<>(roleRepo.findAllById(roleIds));
        db.setRoles(roles);
        return db;
    }
}
