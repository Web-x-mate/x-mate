// src/main/java/xmate/com/service/customer/impl/SegmentServiceImpl.java
package xmate.com.service.customer.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.customer.Segment;
import xmate.com.repo.customer.SegmentRepository;
import xmate.com.service.customer.SegmentService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SegmentServiceImpl implements SegmentService {

    private final SegmentRepository repo;

    @Override @Transactional(readOnly = true)
    public Page<Segment> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Override @Transactional(readOnly = true)
    public List<Segment> all() { return repo.findAll(); }

    @Override @Transactional(readOnly = true)
    public Segment get(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Segment not found"));
    }

    @Override
    public Segment create(Segment s) {
        if (s.getName() == null || s.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (repo.existsByNameIgnoreCase(s.getName())) {
            throw new IllegalArgumentException("Segment name already exists");
        }
        s.setId(null);
        return repo.save(s);
    }

    @Override
    public Segment update(Long id, Segment s) {
        Segment db = get(id);
        String newName = s.getName();
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (!db.getName().equalsIgnoreCase(newName) && repo.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("Segment name already exists");
        }
        db.setName(newName);
        db.setDescription(s.getDescription());
        return repo.save(db);
    }

    @Override
    public void delete(Long id) { repo.deleteById(id); }
}
