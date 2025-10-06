// src/main/java/xmate/com/service/system/ActivityLogService.java
package xmate.com.service.system;

import org.springframework.data.domain.*;
import xmate.com.domain.system.ActivityLog;

public interface ActivityLogService {
    ActivityLog log(Long actorId, String entityType, Long entityId, String action, String diffJson);

    Page<ActivityLog> search(String q, Pageable pageable);
    Page<ActivityLog> byEntity(String entityType, Long entityId, Pageable pageable);
}
