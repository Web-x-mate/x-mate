// src/main/java/xmate/com/service/system/impl/ActivityLogServiceImpl.java
package xmate.com.service.system.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.system.ActivityLog;
import xmate.com.entity.system.User;
import xmate.com.repo.system.ActivityLogRepository;
import xmate.com.repo.system.UserRepository;
import xmate.com.service.system.ActivityLogService;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository repo;
    private final UserRepository userRepo;

    @Override
    public ActivityLog log(Long actorId, String entityType, Long entityId, String action, String diffJson) {
        User actor = null;
        if (actorId != null) {
            actor = userRepo.findById(actorId).orElse(null);
        }
        ActivityLog log = ActivityLog.builder()
                .actor(actor)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .diffJson(diffJson)
                .build();
        return repo.save(log);
    }

    @Override
    public Page<ActivityLog> search(String q, Pageable pageable) {
        return repo.search(q, pageable);
    }

    @Override
    public Page<ActivityLog> byEntity(String entityType, Long entityId, Pageable pageable) {
        return repo.findByEntity(entityType, entityId, pageable);
    }
}
