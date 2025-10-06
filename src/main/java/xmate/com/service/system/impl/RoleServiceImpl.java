// src/main/java/xmate/com/service/system/impl/RoleServiceImpl.java
package xmate.com.service.system.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.system.Permission;
import xmate.com.domain.system.Role;
import xmate.com.repo.system.PermissionRepository;
import xmate.com.repo.system.RoleRepository;
import xmate.com.service.system.RoleService;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    @Override
    public Page<Role> search(String q, Pageable pageable) {
        return roleRepo.search(q, pageable);
    }

    @Override
    public Role get(Long id) {
        return roleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Role not found"));
    }

    @Override
    public Role create(Role r) {
        return roleRepo.save(r);
    }

    @Override
    public Role update(Long id, Role r) {
        Role db = get(id);
        db.setName(r.getName());
        db.setDescription(r.getDescription());
        return db;
    }

    @Override
    public void delete(Long id) {
        roleRepo.deleteById(id);
    }

    @Override
    public Role assignPermissions(Long roleId, Set<Long> permIds) {
        Role role = get(roleId);
        Set<Permission> perms = new HashSet<>(permRepo.findAllById(permIds));
        role.setPermissions(perms);
        return role;
    }
}
