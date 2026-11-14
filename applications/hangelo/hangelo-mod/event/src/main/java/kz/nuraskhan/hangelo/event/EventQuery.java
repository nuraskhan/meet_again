package kz.nuraskhan.hangelo.event;

import static kz.nuraskhan.hangelo.jooq.Tables.EVENT;
import static kz.nuraskhan.hangelo.jooq.Tables.EVENT_SUGGESTION;
import static kz.nuraskhan.hangelo.jooq.Tables.EVENT_TIME_OPTION;
import static kz.nuraskhan.hangelo.jooq.Tables.INVITATION;
import static kz.nuraskhan.hangelo.jooq.Tables.PARTICIPANT_PREFERENCE;
import static kz.nuraskhan.hangelo.jooq.Tables.PLACE;
import static kz.nuraskhan.hangelo.jooq.Tables.PROFILE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kz.nuraskhan.hangelo.event.dto.EventDto;
import kz.nuraskhan.hangelo.event.dto.InvitationDto;
import kz.nuraskhan.hangelo.event.dto.InviteeDto;
import kz.nuraskhan.hangelo.event.dto.PreferenceDto;
import kz.nuraskhan.hangelo.event.dto.SuggestionDto;
import kz.nuraskhan.hangelo.event.dto.PublicEventDto;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
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

  /** The event, if the caller created it or is a guest of it. */
  public Optional<EventDto> get(Long me, Long eventId) {
    return events(me, EVENT.ID.eq(eventId).and(visibleTo(me))).stream().findFirst();
  }

  /** Events the caller created or is a guest of, newest first. */
  public List<EventDto> list(Long me) {
    return events(me, visibleTo(me));
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

    return events(me, EVENT.ID.in(invitationIdByEventId.keySet())).stream()
        .map(event -> new InvitationDto(invitationIdByEventId.get(event.id()), event))
        .toList();
  }

  /** What anyone with the link sees. Needs no caller: it is read without logging in. */
  public Optional<PublicEventDto> byShareToken(String shareToken) {
    var row = dsl
        .select(EVENT.asterisk(), PROFILE.DISPLAY_NAME)
        .from(EVENT)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(EVENT.CREATOR__USER_ACCOUNT_ID))
        .where(EVENT.SHARE_TOKEN.eq(shareToken))
        .fetchOne();

    if (row == null) {
      return Optional.empty();
    }

    Long eventId = row.get(EVENT.ID);

    // A request to join is not an invitation.
    int invitedCount = dsl.fetchCount(INVITATION,
        INVITATION.EVENT_ID.eq(eventId).and(INVITATION.STATUS.ne(InvitationStatus.REQUESTED)));
    int goingCount = dsl.fetchCount(INVITATION,
        INVITATION.EVENT_ID.eq(eventId).and(INVITATION.STATUS.eq(InvitationStatus.ACCEPTED)));

    return Optional.of(new PublicEventDto(
        row.get(EVENT.EVENT_TYPE),
        row.get(EVENT.TITLE),
        row.get(EVENT.DESCRIPTION),
        row.get(EVENT.DISTANCE_MIN_MI),
        row.get(EVENT.DISTANCE_MAX_MI),
        row.get(EVENT.PRICE_MIN),
        row.get(EVENT.PRICE_MAX),
        proposedTimes(List.of(eventId)).getOrDefault(eventId, List.of()),
        row.get(EVENT.STATUS),
        row.get(PROFILE.DISPLAY_NAME),
        invitedCount,
        goingCount));
  }

  private static Condition visibleTo(Long me) {
    return EVENT.CREATOR__USER_ACCOUNT_ID.eq(me)
        .or(DSL.exists(DSL
            .selectOne()
            .from(INVITATION)
            .where(INVITATION.EVENT_ID.eq(EVENT.ID))
            .and(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(me))
            // Someone the creator removed is out: the event is no longer theirs to open.
            // Someone who only asked to join is not in yet: the feed shows them what is public.
            .and(INVITATION.STATUS.notIn(InvitationStatus.REMOVED, InvitationStatus.REQUESTED))));
  }

  /**
   * Five queries whatever the number of events: the events, the guests of all of them, the
   * proposed dates of all of them, the caller's own preferences for them, the suggested places.
   */
  private List<EventDto> events(Long me, Condition condition) {
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

    List<Long> eventIds = eventRows.getValues(EVENT.ID);

    // A guest's preference, if they gave one. Of the place only the typed text and the fact that
    // coordinates exist leave the server; the coordinates themselves go to their owner alone.
    Map<Long, List<InviteeDto>> inviteesByEventId = dsl
        .select(INVITATION.EVENT_ID, INVITATION.ID, PROFILE.USER_ACCOUNT_ID, PROFILE.DISPLAY_NAME,
            INVITATION.STATUS, INVITATION.RESPONDED_AT,
            PARTICIPANT_PREFERENCE.TRAVEL_RADIUS_MI, PARTICIPANT_PREFERENCE.LOCATION_TEXT,
            PARTICIPANT_PREFERENCE.LATITUDE)
        .from(INVITATION)
        .join(PROFILE).on(PROFILE.USER_ACCOUNT_ID.eq(INVITATION.INVITEE__USER_ACCOUNT_ID))
        .leftJoin(PARTICIPANT_PREFERENCE)
        .on(PARTICIPANT_PREFERENCE.EVENT_ID.eq(INVITATION.EVENT_ID))
        .and(PARTICIPANT_PREFERENCE.USER_ACCOUNT_ID.eq(INVITATION.INVITEE__USER_ACCOUNT_ID))
        .where(INVITATION.EVENT_ID.in(eventIds))
        .orderBy(INVITATION.ID)
        .fetchGroups(INVITATION.EVENT_ID, row -> new InviteeDto(
            row.get(INVITATION.ID),
            row.get(PROFILE.USER_ACCOUNT_ID),
            row.get(PROFILE.DISPLAY_NAME),
            row.get(INVITATION.STATUS),
            row.get(INVITATION.RESPONDED_AT),
            row.get(PARTICIPANT_PREFERENCE.TRAVEL_RADIUS_MI),
            row.get(PARTICIPANT_PREFERENCE.LOCATION_TEXT),
            row.get(PARTICIPANT_PREFERENCE.LATITUDE) != null));

    Map<Long, List<Instant>> timesByEventId = proposedTimes(eventIds);

    Map<Long, PreferenceDto> myPreferenceByEventId = dsl
        .select(PARTICIPANT_PREFERENCE.EVENT_ID, PARTICIPANT_PREFERENCE.LATITUDE,
            PARTICIPANT_PREFERENCE.LONGITUDE, PARTICIPANT_PREFERENCE.LOCATION_TEXT,
            PARTICIPANT_PREFERENCE.TRAVEL_RADIUS_MI)
        .from(PARTICIPANT_PREFERENCE)
        .where(PARTICIPANT_PREFERENCE.EVENT_ID.in(eventIds))
        .and(PARTICIPANT_PREFERENCE.USER_ACCOUNT_ID.eq(me))
        .fetchMap(PARTICIPANT_PREFERENCE.EVENT_ID, row -> new PreferenceDto(
            row.get(PARTICIPANT_PREFERENCE.LATITUDE),
            row.get(PARTICIPANT_PREFERENCE.LONGITUDE),
            row.get(PARTICIPANT_PREFERENCE.LOCATION_TEXT),
            row.get(PARTICIPANT_PREFERENCE.TRAVEL_RADIUS_MI)));

    // The places suggested, as they were saved. Only the caller's own distance is worked out now,
    // from the caller's own saved coordinates: nobody else's location takes part in the answer.
    Map<Long, List<SuggestionDto>> suggestionsByEventId = dsl
        .select(EVENT_SUGGESTION.asterisk(), PLACE.asterisk())
        .from(EVENT_SUGGESTION)
        .join(PLACE).on(PLACE.ID.eq(EVENT_SUGGESTION.PLACE_ID))
        .where(EVENT_SUGGESTION.EVENT_ID.in(eventIds))
        .orderBy(EVENT_SUGGESTION.RANK)
        .fetchGroups(EVENT_SUGGESTION.EVENT_ID, row -> {
          PreferenceDto mine = myPreferenceByEventId.get(row.get(EVENT_SUGGESTION.EVENT_ID));
          Double myDistanceMi = mine == null || mine.latitude() == null
              ? null
              : Geo.round1(Geo.distanceMi(mine.latitude(), mine.longitude(),
                  row.get(PLACE.LATITUDE), row.get(PLACE.LONGITUDE)));

          return new SuggestionDto(
              row.get(EVENT_SUGGESTION.RANK),
              row.get(PLACE.ID),
              row.get(PLACE.NAME),
              row.get(PLACE.CATEGORY),
              row.get(PLACE.DESCRIPTION),
              row.get(PLACE.ADDRESS),
              row.get(PLACE.LATITUDE),
              row.get(PLACE.LONGITUDE),
              row.get(PLACE.RATING),
              row.get(PLACE.PRICE_MIN),
              row.get(PLACE.PRICE_MAX),
              row.get(PLACE.WEBSITE),
              row.get(PLACE.PHONE),
              row.get(EVENT_SUGGESTION.AVERAGE_DISTANCE_MI),
              row.get(EVENT_SUGGESTION.FARTHEST_DISTANCE_MI),
              row.get(EVENT_SUGGESTION.WITHIN_EVERYONES_REACH),
              row.get(EVENT_SUGGESTION.FITS_BUDGET),
              row.get(EVENT_SUGGESTION.PARTICIPANTS_CONSIDERED),
              myDistanceMi,
              row.get(PLACE.SAMPLE));
        });

    List<EventDto> result = new ArrayList<>();
    for (var row : eventRows) {
      Long eventId = row.get(EVENT.ID);
      Long creatorId = row.get(EVENT.CREATOR__USER_ACCOUNT_ID);
      result.add(new EventDto(
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
          row.get(EVENT.VISIBILITY),
          row.get(EVENT.CREATED_AT),
          row.get(EVENT.CHAT_ID),
          // The link is the creator's to hand out.
          creatorId.equals(me) ? row.get(EVENT.SHARE_TOKEN) : null,
          creatorId,
          row.get(PROFILE.DISPLAY_NAME),
          // Who is asking to join is the creator's business alone, until the creator says yes.
          inviteesByEventId.getOrDefault(eventId, List.of()).stream()
              .filter(invitee -> creatorId.equals(me) || invitee.status() != InvitationStatus.REQUESTED)
              .toList(),
          myPreferenceByEventId.get(eventId),
          row.get(EVENT.SUGGESTED_AT),
          suggestionsByEventId.getOrDefault(eventId, List.of())));
    }
    return result;
  }

  /** The proposed dates of each event, earliest first. Events without dates are absent. */
  Map<Long, List<Instant>> proposedTimes(Collection<Long> eventIds) {
    return dsl
        .select(EVENT_TIME_OPTION.EVENT_ID, EVENT_TIME_OPTION.STARTS_AT)
        .from(EVENT_TIME_OPTION)
        .where(EVENT_TIME_OPTION.EVENT_ID.in(eventIds))
        .orderBy(EVENT_TIME_OPTION.STARTS_AT)
        .fetchGroups(EVENT_TIME_OPTION.EVENT_ID, EVENT_TIME_OPTION.STARTS_AT);
  }
}
