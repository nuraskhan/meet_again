package kz.nuraskhan.meet_again.model.repository;

import java.time.Instant;
import kz.nuraskhan.meet_again.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  /**
   * Marks one notification of this person as read. The person is part of the condition: somebody
   * else's notification is simply not found.
   *
   * @return 1 if it was theirs and unread, 0 otherwise
   */
  @Modifying
  @Query("""
      update Notification n set n.readAt = :now
      where n.id = :id and n.userAccountId = :userAccountId and n.readAt is null
      """)
  int markRead(@Param("id") Long id, @Param("userAccountId") Long userAccountId, @Param("now") Instant now);

  /** Marks every unread notification of this person as read. */
  @Modifying
  @Query("update Notification n set n.readAt = :now where n.userAccountId = :userAccountId and n.readAt is null")
  int markAllRead(@Param("userAccountId") Long userAccountId, @Param("now") Instant now);
}
