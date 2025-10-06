// src/main/java/xmate/com/domain/system/ActivityLog.java
package xmate.com.domain.system;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity @Table(name = "activity_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name="entity_type", length = 50)
    private String entityType; // ORDER / PRODUCT / INVENTORY / ...

    @Column(name="entity_id")
    private Long entityId;

    @Column(length = 50)
    private String action; // CREATE / UPDATE / DELETE / ...

    @Column(name="diff_json", columnDefinition = "json")
    private String diffJson;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
