// src/main/java/xmate/com/service/system/impl/PermissionServiceImpl.java
package xmate.com.service.system.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.domain.system.Permission;
import xmate.com.repo.system.PermissionRepository;
import xmate.com.service.system.PermissionService;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository repo;

    @Override
    public Page<Permission> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Override
    public Permission get(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Permission not found"));
    }

    @Override
    public Permission create(Permission p) {
        return repo.save(p);
    }

    @Override
    public Permission update(Long id, Permission p) {
        Permission db = get(id);
        db.setKey(p.getKey());
        return db;
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
