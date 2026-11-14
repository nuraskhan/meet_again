package kz.nuraskhan.meet_again.magain.notification;

import static kz.nuraskhan.meet_again.jooq.Tables.NOTIFICATION;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;

import java.util.List;
import kz.nuraskhan.meet_again.magain.notification.dto.NotificationDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading a person's notifications. */
@Component
public class NotificationQuery {

  private final DSLContext dsl;

  public NotificationQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /**
   * A page of the caller's notifications, newest first.
   *
   * @param beforeId only notifications older than this one; null = from the newest
   */
  public List<NotificationDto> page(Long me, Long beforeId, int limit) {
    Condition older = beforeId == null ? DSL.noCondition() : NOTIFICATION.ID.lt(beforeId);

    return dsl
        .select(NOTIFICATION.ID, NOTIFICATION.KIND, NOTIFICATION.TEXT, NOTIFICATION.ACTOR__USER_ACCOUNT_ID,
            PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL, NOTIFICATION.EVENT_ID, NOTIFICATION.CREATED_AT,
            NOTIFICATION.READ_AT)
        .from(NOTIFICATION)
        // The actor's name and picture as they are now. Left join: a notification may have no actor.
        .leftJoin(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(NOTIFICATION.ACTOR__USER_ACCOUNT_ID))
        .where(NOTIFICATION.USER_ACCOUNT_ID.eq(me))
        .and(older)
        .orderBy(NOTIFICATION.ID.desc())
        .limit(limit)
        .fetch(row -> new NotificationDto(
            row.get(NOTIFICATION.ID),
            row.get(NOTIFICATION.KIND),
            row.get(NOTIFICATION.TEXT),
            row.get(NOTIFICATION.ACTOR__USER_ACCOUNT_ID),
            row.get(PROFILE.DISPLAY_NAME),
            row.get(PROFILE.AVATAR_URL),
            row.get(NOTIFICATION.EVENT_ID),
            row.get(NOTIFICATION.CREATED_AT),
            row.get(NOTIFICATION.READ_AT) != null));
  }

  /** Is there such a notification, and is it the caller's. */
  public boolean isMine(Long me, Long notificationId) {
    return dsl.fetchExists(NOTIFICATION,
        NOTIFICATION.ID.eq(notificationId).and(NOTIFICATION.USER_ACCOUNT_ID.eq(me)));
  }

  public int unreadCount(Long me) {
    return dsl.fetchCount(NOTIFICATION,
        NOTIFICATION.USER_ACCOUNT_ID.eq(me).and(NOTIFICATION.READ_AT.isNull()));
  }
}
