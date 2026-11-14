package kz.nuraskhan.meet_again.magain.event;

import static kz.nuraskhan.meet_again.jooq.Tables.EVENT;
import static kz.nuraskhan.meet_again.jooq.Tables.INVITATION;
import static kz.nuraskhan.meet_again.jooq.Tables.PROFILE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.nuraskhan.meet_again.magain.event.dto.EventDto;
import kz.nuraskhan.meet_again.magain.event.dto.InvitationDto;
import kz.nuraskhan.meet_again.magain.event.dto.InviteeDto;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Reading events and invitations. */
@Component
public class EventQuery {

  private final DSLContext dsl;

  public EventQuery(DSLContext dsl) {
    this.dsl = dsl;
  }

  /** The event, if the caller created it or is invited to it. */
  public Optional<EventDto> get(Long me, Long eventId) {
    return events(EVENT.ID.eq(eventId).and(visibleTo(me))).stream().findFirst();
  }

  /** Events the caller created or is invited to, newest first. */
  public List<EventDto> list(Long me) {
    return events(visibleTo(me));
  }

  /** Invitations to active events the caller has not answered, newest first. */
  public List<InvitationDto> incoming(Long me) {
    Map<Long, Long> invitationIdByEventId = dsl
        .select(INVITATION.EVENT_ID, INVITATION.ID)
        .from(INVITATION)
        .join(EVENT).on(EVENT.ID.eq(INVITATION.EVENT_ID))
        .where(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(me))
        .and(INVITATION.STATUS.eq(InvitationStatus.PENDING))
        .and(EVENT.STATUS.eq(EventStatus.ACTIVE))
        .fetchMap(INVITATION.EVENT_ID, INVITATION.ID);

    if (invitationIdByEventId.isEmpty()) {
      return List.of();
    }

    return events(EVENT.ID.in(invitationIdByEventId.keySet())).stream()
        .map(event -> new InvitationDto(invitationIdByEventId.get(event.id()), event))
        .toList();
  }

  private static Condition visibleTo(Long me) {
    return EVENT.CREATOR__USER_ACCOUNT_ID.eq(me)
        .or(DSL.exists(DSL
            .selectOne()
            .from(INVITATION)
            .where(INVITATION.EVENT_ID.eq(EVENT.ID))
            .and(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(me))));
  }

  /** Two queries whatever the number of events: the events, then the invitees of all of them. */
  private List<EventDto> events(Condition condition) {
    var eventRows = dsl
        .select(EVENT.asterisk(), PROFILE.DISPLAY_NAME)
        .from(EVENT)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(EVENT.CREATOR__USER_ACCOUNT_ID))
        .where(condition)
        .orderBy(EVENT.ID.desc())
        .fetch();

    if (eventRows.isEmpty()) {
      return List.of();
    }

    Map<Long, List<InviteeDto>> inviteesByEventId = dsl
        .select(INVITATION.EVENT_ID, INVITATION.ID, PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME,
            INVITATION.STATUS, INVITATION.RESPONDED_AT)
        .from(INVITATION)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(INVITATION.INVITEE__USER_ACCOUNT_ID))
        .where(INVITATION.EVENT_ID.in(eventRows.getValues(EVENT.ID)))
        .orderBy(INVITATION.ID)
        .fetchGroups(INVITATION.EVENT_ID, row -> new InviteeDto(
            row.get(INVITATION.ID),
            row.get(PROFILE.USER_ACCOUNT_ID),
            row.get(PROFILE.DISPLAY_NAME),
            row.get(INVITATION.STATUS),
            row.get(INVITATION.RESPONDED_AT)));

    List<EventDto> result = new ArrayList<>();
    for (var row : eventRows) {
      Long eventId = row.get(EVENT.ID);
      result.add(new EventDto(
          eventId,
          row.get(EVENT.EVENT_TYPE),
          row.get(EVENT.TITLE),
          row.get(EVENT.DESCRIPTION),
          row.get(EVENT.DISTANCE_MIN_KM),
          row.get(EVENT.DISTANCE_MAX_KM),
          row.get(EVENT.PRICE_MIN),
          row.get(EVENT.PRICE_MAX),
          row.get(EVENT.STARTS_AT),
          row.get(EVENT.STATUS),
          row.get(EVENT.CREATED_AT),
          row.get(EVENT.CHAT_ID),
          row.get(EVENT.CREATOR__USER_ACCOUNT_ID),
          row.get(PROFILE.DISPLAY_NAME),
          inviteesByEventId.getOrDefault(eventId, List.of())));
    }
    return result;
  }
}
