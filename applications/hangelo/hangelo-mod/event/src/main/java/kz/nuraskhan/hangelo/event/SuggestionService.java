package kz.nuraskhan.hangelo.event;

import static kz.nuraskhan.hangelo.jooq.Tables.INVITATION;
import static kz.nuraskhan.hangelo.jooq.Tables.PARTICIPANT_PREFERENCE;
import static kz.nuraskhan.hangelo.jooq.Tables.PLACE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import kz.nuraskhan.hangelo.model.entity.Event;
import kz.nuraskhan.hangelo.model.entity.EventSuggestion;
import kz.nuraskhan.hangelo.model.entity.Invitation;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import kz.nuraskhan.hangelo.model.repository.EventRepository;
import kz.nuraskhan.hangelo.model.repository.EventSuggestionRepository;
import kz.nuraskhan.hangelo.model.repository.InvitationRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suggests where to go: places of the event's category that are near the people coming, the best
 * rated first.
 *
 * <p><b>Whose location counts.</b> The creator and every guest who has not declined - of them,
 * those who gave coordinates (a pin on the map or the location of their device). A place typed as
 * text has no coordinates and cannot be measured.
 *
 * <p><b>Which places.</b> Of the same category as the event (any category for an event of the type
 * OTHER), not deactivated, inside a box around those people.
 *
 * <p><b>The order.</b>
 * <ol>
 *   <li>within everyone's own travel radius, before those that are a stretch for someone;
 *   <li>prices overlapping the budget of the event, before those that do not;
 *   <li>the higher rating;
 *   <li>the smaller average distance.
 * </ol>
 * The first {@link #HOW_MANY} are kept. If nothing is within everyone's reach, the nearest ones
 * are still offered, marked as a stretch - better than an empty answer.
 *
 * <p><b>Kept, not recomputed.</b> The result is saved with the event and shown as saved on every
 * opening. It changes only when someone asks again, or the first time everyone's location is known.
 */
@Service
public class SuggestionService {

  /** How many places are offered. */
  static final int HOW_MANY = 3;

  private final EventRepository eventRepository;
  private final InvitationRepository invitationRepository;
  private final EventSuggestionRepository eventSuggestionRepository;
  private final DSLContext dsl;

  public SuggestionService(EventRepository eventRepository,
                           InvitationRepository invitationRepository,
                           EventSuggestionRepository eventSuggestionRepository,
                           DSLContext dsl) {
    this.eventRepository = eventRepository;
    this.invitationRepository = invitationRepository;
    this.eventSuggestionRepository = eventSuggestionRepository;
    this.dsl = dsl;
  }

  /** A person with coordinates, and how far they are ready to go. */
  private record Person(double latitude, double longitude, int travelRadiusMi) {
  }

  private record Candidate(Long placeId, double rating, double averageMi, double farthestMi,
                           boolean withinEveryonesReach, boolean fitsBudget) {
  }

  /**
   * "Suggest places" pressed by a participant: computes anew and replaces what was saved.
   *
   * @throws ApiException 409 {@code no_coordinates} if no participant has given coordinates yet
   */
  @Transactional
  public void refresh(Long me, Long eventId) {
    Event event = eventRepository.findById(eventId).orElse(null);
    if (event == null || !isParticipant(event, me)) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }
    EventService.requireActive(event);

    List<Person> people = peopleWithCoordinates(event);
    if (people.isEmpty()) {
      throw ApiException.conflict("no_coordinates",
          "Nobody has put their location on the map yet. Add yours first");
    }

    compute(event, people);
  }

  /**
   * Called when someone saves their location: suggests by itself the first time the location of
   * everyone coming is known - the creator and the guests who said yes. Does nothing if suggestions
   * were already computed (they change only on request), or someone's location is still missing.
   */
  @Transactional
  public void suggestIfEveryoneIsKnown(Long eventId) {
    Event event = eventRepository.findById(eventId).orElse(null);
    if (event == null || event.status != EventStatus.ACTIVE || event.suggestedAt != null) {
      return;
    }

    int coming = 1 + dsl.fetchCount(INVITATION,
        INVITATION.EVENT_ID.eq(eventId).and(INVITATION.STATUS.eq(InvitationStatus.ACCEPTED)));

    int comingWithCoordinates = dsl.fetchCount(PARTICIPANT_PREFERENCE,
        PARTICIPANT_PREFERENCE.EVENT_ID.eq(eventId)
            .and(PARTICIPANT_PREFERENCE.LATITUDE.isNotNull())
            .and(PARTICIPANT_PREFERENCE.USER_ACCOUNT_ID.eq(event.creatorUserAccountId)
                .or(guestWith(eventId, InvitationStatus.ACCEPTED))));

    if (comingWithCoordinates == coming) {
      compute(event, peopleWithCoordinates(event));
    }
  }

  private void compute(Event event, List<Person> people) {
    List<Candidate> best = rank(event, people).stream().limit(HOW_MANY).toList();

    Instant now = Instant.now();

    eventSuggestionRepository.deleteAllOfEvent(event.id);

    int rank = 1;
    for (Candidate candidate : best) {
      EventSuggestion suggestion = new EventSuggestion();
      suggestion.eventId = event.id;
      suggestion.placeId = candidate.placeId();
      suggestion.rank = rank++;
      suggestion.averageDistanceMi = Geo.round1(candidate.averageMi());
      suggestion.farthestDistanceMi = Geo.round1(candidate.farthestMi());
      suggestion.withinEveryonesReach = candidate.withinEveryonesReach();
      suggestion.fitsBudget = candidate.fitsBudget();
      suggestion.participantsConsidered = people.size();
      suggestion.createdAt = now;
      eventSuggestionRepository.save(suggestion);
    }

    // The delete above cleared the persistence context: take the event anew before changing it.
    eventRepository.findById(event.id).orElseThrow().suggestedAt = now;
  }

  /** Every suitable place near these people, the best first. */
  private List<Candidate> rank(Event event, List<Person> people) {
    // A box that holds everything any of them could reach: their own bounding box, widened by the
    // largest travel radius. Narrowing in the database keeps the measuring below small.
    double minLat = people.stream().mapToDouble(Person::latitude).min().orElseThrow();
    double maxLat = people.stream().mapToDouble(Person::latitude).max().orElseThrow();
    double minLon = people.stream().mapToDouble(Person::longitude).min().orElseThrow();
    double maxLon = people.stream().mapToDouble(Person::longitude).max().orElseThrow();
    int largestRadius = people.stream().mapToInt(Person::travelRadiusMi).max().orElseThrow();

    double latMargin = largestRadius / Geo.MILES_PER_DEGREE;
    // A degree of longitude shrinks towards the poles. The floor keeps the margin finite there.
    double cos = Math.max(0.1, Math.cos(Math.toRadians((minLat + maxLat) / 2)));
    double lonMargin = latMargin / cos;

    // An event of the type OTHER has no category of its own: any place may suit.
    Condition category = event.eventType == EventType.OTHER
        ? DSL.noCondition()
        : PLACE.CATEGORY.eq(event.eventType);

    List<Candidate> candidates = new ArrayList<>();

    dsl.select(PLACE.ID, PLACE.LATITUDE, PLACE.LONGITUDE, PLACE.RATING, PLACE.PRICE_MIN, PLACE.PRICE_MAX)
        .from(PLACE)
        .where(PLACE.DEACTIVATED_AT.isNull())
        .and(category)
        .and(PLACE.LATITUDE.between(minLat - latMargin, maxLat + latMargin))
        .and(PLACE.LONGITUDE.between(minLon - lonMargin, maxLon + lonMargin))
        .forEach(row -> {
          double total = 0;
          double farthest = 0;
          boolean withinEveryonesReach = true;

          for (Person person : people) {
            double miles = Geo.distanceMi(person.latitude(), person.longitude(),
                row.get(PLACE.LATITUDE), row.get(PLACE.LONGITUDE));
            total += miles;
            farthest = Math.max(farthest, miles);
            if (miles > person.travelRadiusMi()) {
              withinEveryonesReach = false;
            }
          }

          // A place whose prices are not known is not judged by the budget: leaving it out for
          // what we do not know would hide most real venues.
          Integer placeMin = row.get(PLACE.PRICE_MIN);
          Integer placeMax = row.get(PLACE.PRICE_MAX);
          boolean fitsBudget = placeMin == null || placeMax == null
              || (placeMin <= event.priceMax && placeMax >= event.priceMin);

          // Not rated counts as the lowest rating: among places that suit, a rated one goes first.
          Double rated = row.get(PLACE.RATING);

          candidates.add(new Candidate(row.get(PLACE.ID), rated == null ? 0 : rated,
              total / people.size(), farthest, withinEveryonesReach, fitsBudget));
        });

    candidates.sort(Comparator
        .comparing(Candidate::withinEveryonesReach).reversed()
        .thenComparing(Comparator.comparing(Candidate::fitsBudget).reversed())
        .thenComparing(Comparator.comparingDouble(Candidate::rating).reversed())
        .thenComparingDouble(Candidate::averageMi)
        .thenComparing(Candidate::placeId));

    return candidates;
  }

  /** The creator and the guests who have not declined - those of them who gave coordinates. */
  private List<Person> peopleWithCoordinates(Event event) {
    return dsl
        .select(PARTICIPANT_PREFERENCE.LATITUDE, PARTICIPANT_PREFERENCE.LONGITUDE,
            PARTICIPANT_PREFERENCE.TRAVEL_RADIUS_MI)
        .from(PARTICIPANT_PREFERENCE)
        .where(PARTICIPANT_PREFERENCE.EVENT_ID.eq(event.id))
        .and(PARTICIPANT_PREFERENCE.LATITUDE.isNotNull())
        .and(PARTICIPANT_PREFERENCE.USER_ACCOUNT_ID.eq(event.creatorUserAccountId)
            .or(guestWith(event.id, InvitationStatus.PENDING, InvitationStatus.ACCEPTED)))
        .fetch(row -> new Person(row.value1(), row.value2(), row.value3()));
  }

  /** "The owner of this preference is a guest of the event with one of these answers." */
  private static Condition guestWith(Long eventId, InvitationStatus... statuses) {
    return DSL.exists(DSL
        .selectOne()
        .from(INVITATION)
        .where(INVITATION.EVENT_ID.eq(eventId))
        .and(INVITATION.INVITEE__USER_ACCOUNT_ID.eq(PARTICIPANT_PREFERENCE.USER_ACCOUNT_ID))
        .and(INVITATION.STATUS.in(statuses)));
  }

  private boolean isParticipant(Event event, Long userId) {
    if (event.creatorUserAccountId.equals(userId)) {
      return true;
    }
    Invitation invitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(event.id, userId).orElse(null);
    return invitation != null && invitation.status.isIn();
  }
}
