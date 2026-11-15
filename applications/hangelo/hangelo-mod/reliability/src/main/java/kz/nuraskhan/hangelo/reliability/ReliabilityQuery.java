package kz.nuraskhan.hangelo.reliability;

import static kz.nuraskhan.hangelo.jooq.Tables.EVENT;
import static kz.nuraskhan.hangelo.jooq.Tables.INVITATION;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import kz.nuraskhan.hangelo.jooq.tables.Invitation;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Counts a person's reliability from the history of events. Nothing is stored: the rating is what
 * the history says today.
 *
 * <p>A promise is saying "coming" to an event, or creating one. It is
 * <ul>
 *   <li>kept, when the event took place (its date passed, status EXPIRED) and the person was still
 *       in - and, for a guest, the creator did not mark them as absent;
 *   <li>broken, when the guest left after saying "coming", or was marked as absent, or the creator
 *       cancelled an event somebody was already coming to.
 * </ul>
 * Events that are still ahead count for nothing yet. Declining an invitation is not a broken
 * promise: nothing was promised.
 */
@Component
public class ReliabilityQuery {

  /** Fewer promises than this say too little: one cancelled plan must not brand a newcomer. */
  public static final int ENOUGH = 3;

  private final DSLContext dsl;

  public ReliabilityQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  public ReliabilityDto of(Long userId) {
    return of(java.util.List.of(userId)).getOrDefault(userId, ReliabilityDto.NONE);
  }

  /** The rating of each of these people; someone with no history is absent from the map. */
  public Map<Long, ReliabilityDto> of(Collection<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }

    Map<Long, int[]> counts = new HashMap<>(); // userId -> {kept, broken}

    // As a guest.
    var kept = DSL.count().filterWhere(
        INVITATION.STATUS.eq(InvitationStatus.ACCEPTED)
            .and(INVITATION.NO_SHOW.isFalse())
            .and(EVENT.STATUS.eq(EventStatus.EXPIRED)));
    var broken = DSL.count().filterWhere(
        INVITATION.LEFT_AT.isNotNull()
            .or(INVITATION.STATUS.eq(InvitationStatus.ACCEPTED).and(INVITATION.NO_SHOW.isTrue())));

    dsl.select(INVITATION.INVITEE__USER_ACCOUNT_ID, kept, broken)
        .from(INVITATION)
        .join(EVENT).on(EVENT.ID.eq(INVITATION.EVENT_ID))
        .where(INVITATION.INVITEE__USER_ACCOUNT_ID.in(userIds))
        .groupBy(INVITATION.INVITEE__USER_ACCOUNT_ID)
        .forEach(row -> add(counts, row.value1(), row.value2(), row.value3()));

    // As a creator: an event held is a promise kept; one cancelled while somebody was coming to
    // it is a promise broken. Cancelling what nobody had agreed to lets nobody down.
    Invitation coming = INVITATION.as("coming");
    var held = DSL.count().filterWhere(EVENT.STATUS.eq(EventStatus.EXPIRED));
    var letDown = DSL.count().filterWhere(
        EVENT.STATUS.eq(EventStatus.CANCELLED)
            .and(DSL.exists(DSL.selectOne().from(coming)
                .where(coming.EVENT_ID.eq(EVENT.ID))
                .and(coming.STATUS.eq(InvitationStatus.ACCEPTED)))));

    dsl.select(EVENT.CREATOR__USER_ACCOUNT_ID, held, letDown)
        .from(EVENT)
        .where(EVENT.CREATOR__USER_ACCOUNT_ID.in(userIds))
        .groupBy(EVENT.CREATOR__USER_ACCOUNT_ID)
        .forEach(row -> add(counts, row.value1(), row.value2(), row.value3()));

    Map<Long, ReliabilityDto> result = new HashMap<>();
    counts.forEach((userId, c) -> result.put(userId, rating(c[0], c[1])));
    return result;
  }

  /** The rating for so many promises kept and broken. */
  static ReliabilityDto rating(int kept, int broken) {
    int total = kept + broken;
    Integer score = total < ENOUGH ? null : Math.round(100f * kept / total);
    return new ReliabilityDto(score, kept, broken);
  }

  private static void add(Map<Long, int[]> counts, Long userId, int kept, int broken) {
    int[] c = counts.computeIfAbsent(userId, id -> new int[2]);
    c[0] += kept;
    c[1] += broken;
  }
}
