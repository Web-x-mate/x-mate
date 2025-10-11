// src/main/java/xmate/com/service/procurement/impl/SupplierServiceImpl.java
package xmate.com.service.procurement.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.procurement.Supplier;
import xmate.com.repo.procurement.SupplierRepository;
import xmate.com.service.procurement.SupplierService;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<Supplier> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Supplier get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + id));
    }

    @Override
    public Supplier create(Supplier s) {
        s.setId(null);
        return repo.save(s);
    }

    @Override
    public Supplier update(Long id, Supplier s) {
        Supplier cur = get(id);
        cur.setName(s.getName());
        cur.setEmail(s.getEmail());
        cur.setPhone(s.getPhone());
        cur.setAddress(s.getAddress());
        cur.setNote(s.getNote());
        return repo.save(cur);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }
}
