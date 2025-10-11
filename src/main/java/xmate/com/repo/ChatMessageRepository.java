package xmate.com.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import xmate.com.entity.ChatMessageEntity;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findByRoomOrderByCreatedAtAsc(String room);

    // Lấy danh sách inbox cho admin, nhóm theo ROOM
    @Query(value = """
      SELECT
        CAST(SUBSTRING_INDEX(room,'-',-1) AS UNSIGNED) AS userId,
        MAX(CASE WHEN sender_id <> 0 THEN sender_email END) AS userEmail,
        MAX(created_at) AS time,
        SUBSTRING_INDEX(
          GROUP_CONCAT(content ORDER BY created_at DESC SEPARATOR '\n'),
          '\n', 1
        ) AS preview,
        room AS room
      FROM chat_messages
      WHERE room LIKE 'u-%'
      GROUP BY room
      ORDER BY time DESC
    """, nativeQuery = true)
    List<InboxRow> findAdminInboxRows();

    interface InboxRow {
        Long getUserId();
        String getUserEmail();
        java.sql.Timestamp getTime();
        String getPreview();
        String getRoom();
    }

    // XÓA 1 ROOM
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ChatMessageEntity m WHERE m.room = :room")
    int deleteByRoom(@Param("room") String room);

    // XÓA TẤT CẢ ROOM kiểu u-%
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "DELETE FROM chat_messages WHERE room LIKE 'u-%'", nativeQuery = true)
    int deleteAllAdminThreads();
}
