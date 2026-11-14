package kz.nuraskhan.meet_again.magain.event.reminder;

import static kz.nuraskhan.meet_again.jooq.Tables.EVENT;
import static kz.nuraskhan.meet_again.jooq.Tables.EVENT_REMINDER;
import static kz.nuraskhan.meet_again.jooq.Tables.EVENT_TIME_OPTION;
import static kz.nuraskhan.meet_again.jooq.Tables.INVITATION;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;
import static kz.nuraskhan.meet_again.jooq.Tables.USER_ACCOUNT;

import java.time.Instant;
import java.util.List;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading what the reminder job needs. */
@Component
class ReminderQuery {

  /**
   * An event whose next proposed date is near.
   *
   * @param startsAt     the nearest proposed date that is still ahead
   * @param optionsAhead how many proposed dates are still ahead, that one included
   */
  record DueEvent(Long eventId, Long creatorId, String title, String timeZone, Instant startsAt,
                  Integer optionsAhead) {
  }

  record Recipient(Long userId, String email, String displayName) {
  }

  private final DSLContext dsl;

  ReminderQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** Active events whose nearest date ahead falls within (now, until]. */
  List<DueEvent> dueEvents(Instant now, Instant until) {
    Field<Instant> nearest = DSL.min(EVENT_TIME_OPTION.STARTS_AT);

    return dsl
        .select(EVENT.ID, EVENT.CREATOR__USER_ACCOUNT_ID, EVENT.TITLE, EVENT.TIME_ZONE, nearest, DSL.count())
        .from(EVENT)
        .join(EVENT_TIME_OPTION).on(EVENT_TIME_OPTION.EVENT_ID.eq(EVENT.ID))
        .where(EVENT.STATUS.eq(EventStatus.ACTIVE))
        // Dates already behind do not count: an event with one passed and one coming date is
        // reminded about the coming one.
        .and(EVENT_TIME_OPTION.STARTS_AT.gt(now))
        .groupBy(EVENT.ID, EVENT.CREATOR__USER_ACCOUNT_ID, EVENT.TITLE, EVENT.TIME_ZONE)
        .having(nearest.le(until))
        .orderBy(EVENT.ID)
        .fetch(row -> new DueEvent(row.value1(), row.value2(), row.value3(), row.value4(),
            row.value5(), row.value6()));
  }

  /**
   * Who is to be reminded about the event: the creator and the guests who said yes - of them, those
   * with a confirmed address, an active account, and no reminder for this event yet.
   */
  List<Recipient> recipients(DueEvent event) {
    return dsl
        .select(USER_ACCOUNT.ID, USER_ACCOUNT.EMAIL, PROFILE.DISPLAY_NAME)
        .from(USER_ACCOUNT)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(USER_ACCOUNT.ID))
        .where(USER_ACCOUNT.ID.eq(event.creatorId())
            .or(DSL.exists(DSL
                .selectOne()
                .from(INVITATION)
                .where(INVITATION.EVENT_ID.eq(event.eventId()))
                .and(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(USER_ACCOUNT.ID))
                .and(INVITATION.STATUS.eq(InvitationStatus.ACCEPTED)))))
        // We write only to an address its owner confirmed.
        .and(USER_ACCOUNT.EMAIL_VERIFIED_AT.isNotNull())
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .andNotExists(DSL
            .selectOne()
            .from(EVENT_REMINDER)
            .where(EVENT_REMINDER.EVENT_ID.eq(event.eventId()))
            .and(EVENT_REMINDER.USER_ACCOUNT_ID.eq(USER_ACCOUNT.ID)))
        .orderBy(USER_ACCOUNT.ID)
        .fetch(row -> new Recipient(row.value1(), row.value2(), row.value3()));
  }
}
