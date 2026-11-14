package kz.nuraskhan.hangelo.event;

import static kz.nuraskhan.hangelo.jooq.Tables.EVENT;
import static kz.nuraskhan.hangelo.jooq.Tables.INVITATION;
import static kz.nuraskhan.hangelo.jooq.Tables.PROFILE;
import static kz.nuraskhan.hangelo.jooq.Tables.USER_ACCOUNT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.event.dto.FeedItemDto;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.EventVisibility;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading the feed: the public events that are on, newest first. */
@Component
public class FeedQuery {

  /** How many faces of the guests a feed card carries. The count carries the rest. */
  private static final int FACES = 5;

  private final DSLContext dsl;
  private final EventQuery eventQuery;

  public FeedQuery(DSLContext dsl, EventQuery eventQuery) {
    this.dsl = dsl;
    this.eventQuery = eventQuery;
  }

  /**
   * A page of the feed.
   *
   * @param eventType only events of this type; null = all
   * @param beforeId  only events older than this one (the id of the last one of the previous page);
   *                  null = from the newest
   */
  public List<FeedItemDto> page(Long me, EventType eventType, Long beforeId, int limit) {
    Condition ofType = eventType == null ? DSL.noCondition() : EVENT.EVENT_TYPE.eq(eventType);
    Condition older = beforeId == null ? DSL.noCondition() : EVENT.ID.lt(beforeId);

    var rows = dsl
        .select(EVENT.asterisk(), PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL, PROFILE.CITY)
        .from(EVENT)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(EVENT.CREATOR__USER_ACCOUNT_ID))
        .join(USER_ACCOUNT).on(USER_ACCOUNT.ID.eq(EVENT.CREATOR__USER_ACCOUNT_ID))
        .where(EVENT.VISIBILITY.eq(EventVisibility.PUBLIC))
        .and(EVENT.STATUS.eq(EventStatus.ACTIVE))
        // An event of someone whose account was closed is not offered to anyone.
        .and(USER_ACCOUNT.DEACTIVATED_AT.isNull())
        .and(ofType)
        .and(older)
        .orderBy(EVENT.ID.desc())
        .limit(limit)
        .fetch();

    if (rows.isEmpty()) {
      return List.of();
    }

    List<Long> eventIds = rows.getValues(EVENT.ID);

    Map<Long, List<Instant>> timesByEventId = eventQuery.proposedTimes(eventIds);

    // The guests who are coming, in the order they came.
    Map<Long, List<FeedItemDto.Person>> goingByEventId = dsl
        .select(INVITATION.EVENT_ID, PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME, PROFILE.AVATAR_URL)
        .from(INVITATION)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(INVITATION.INVITEE__USER_ACCOUNT_ID))
        .where(INVITATION.EVENT_ID.in(eventIds))
        .and(INVITATION.STATUS.eq(InvitationStatus.ACCEPTED))
        .orderBy(INVITATION.ID)
        .fetchGroups(INVITATION.EVENT_ID,
            row -> new FeedItemDto.Person(row.value2(), row.value3(), row.value4()));

    // Places taken: coming, or invited by name and not answered yet.
    Map<Long, Integer> takenByEventId = dsl
        .select(INVITATION.EVENT_ID, DSL.count())
        .from(INVITATION)
        .where(INVITATION.EVENT_ID.in(eventIds))
        .and(INVITATION.STATUS.in(InvitationStatus.PENDING, InvitationStatus.ACCEPTED))
        .groupBy(INVITATION.EVENT_ID)
        .fetchMap(INVITATION.EVENT_ID, DSL.count());

    Map<Long, InvitationStatus> myAnswerByEventId = dsl
        .select(INVITATION.EVENT_ID, INVITATION.STATUS)
        .from(INVITATION)
        .where(INVITATION.EVENT_ID.in(eventIds))
        .and(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(me))
        .fetchMap(INVITATION.EVENT_ID, INVITATION.STATUS);

    List<FeedItemDto> result = new ArrayList<>();
    for (var row : rows) {
      Long eventId = row.get(EVENT.ID);
      Long creatorId = row.get(EVENT.CREATOR__USER_ACCOUNT_ID);
      List<FeedItemDto.Person> going = goingByEventId.getOrDefault(eventId, List.of());

      InvitationStatus myAnswer = myAnswerByEventId.get(eventId);
      String myStanding = creatorId.equals(me) ? "CREATOR" : myAnswer == null ? "NONE" : myAnswer.name();

      result.add(new FeedItemDto(
          eventId,
          row.get(EVENT.EVENT_TYPE),
          row.get(EVENT.TITLE),
          row.get(EVENT.DESCRIPTION),
          row.get(EVENT.DISTANCE_MIN_MI),
          row.get(EVENT.DISTANCE_MAX_MI),
          row.get(EVENT.PRICE_MIN),
          row.get(EVENT.PRICE_MAX),
          timesByEventId.getOrDefault(eventId, List.of()),
          row.get(EVENT.STATUS),
          row.get(EVENT.CREATED_AT),
          creatorId,
          row.get(PROFILE.DISPLAY_NAME),
          row.get(PROFILE.AVATAR_URL),
          row.get(PROFILE.CITY),
          going.size(),
          going.stream().limit(FACES).toList(),
          takenByEventId.getOrDefault(eventId, 0) >= EventService.MAX_GUESTS,
          myStanding));
    }
    return result;
  }
}
