package kz.nuraskhan.meet_again.magain.event;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import kz.nuraskhan.meet_again.magain.chat.ChatService;
import kz.nuraskhan.meet_again.magain.event.dto.CreateEventRequest;
import kz.nuraskhan.meet_again.magain.event.dto.SavePreferenceRequest;
import kz.nuraskhan.meet_again.model.entity.Event;
import kz.nuraskhan.meet_again.model.entity.EventTimeOption;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import kz.nuraskhan.meet_again.model.entity.ParticipantPreference;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;
import kz.nuraskhan.meet_again.model.repository.EventRepository;
import kz.nuraskhan.meet_again.model.repository.EventTimeOptionRepository;
import kz.nuraskhan.meet_again.model.repository.FriendshipRepository;
import kz.nuraskhan.meet_again.model.repository.InvitationRepository;
import kz.nuraskhan.meet_again.model.repository.ParticipantPreferenceRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  /** 18 random bytes = 144 bits: the link cannot be guessed. In base64url that is 24 characters. */
  private static final int SHARE_TOKEN_BYTES = 18;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final EventRepository eventRepository;
  private final EventTimeOptionRepository eventTimeOptionRepository;
  private final InvitationRepository invitationRepository;
  private final FriendshipRepository friendshipRepository;
  private final ParticipantPreferenceRepository participantPreferenceRepository;
  private final ChatService chatService;
  private final SuggestionService suggestionService;

  public EventService(EventRepository eventRepository,
                      EventTimeOptionRepository eventTimeOptionRepository,
                      InvitationRepository invitationRepository,
                      FriendshipRepository friendshipRepository,
                      ParticipantPreferenceRepository participantPreferenceRepository,
                      ChatService chatService,
                      SuggestionService suggestionService) {
    this.eventRepository = eventRepository;
    this.eventTimeOptionRepository = eventTimeOptionRepository;
    this.invitationRepository = invitationRepository;
    this.friendshipRepository = friendshipRepository;
    this.participantPreferenceRepository = participantPreferenceRepository;
    this.chatService = chatService;
    this.suggestionService = suggestionService;
  }

  /**
   * Creates the event with its link, the proposed dates, an invitation for every invitee, and the
   * group chat with the creator and all the invitees in it - all or nothing.
   *
   * @return id of the event
   */
  @Transactional
  public Long create(Long me, CreateEventRequest request) {
    EventType type = request.eventType();

    // What the form leaves out comes from the type: that is the "pre-filled" part.
    String title = isBlank(request.title()) ? type.defaultTitle : request.title().trim();
    int distanceMinMi = orDefault(request.distanceMinMi(), type.defaultDistanceMinMi);
    int distanceMaxMi = orDefault(request.distanceMaxMi(), type.defaultDistanceMaxMi);
    int priceMin = orDefault(request.priceMin(), type.defaultPriceMin);
    int priceMax = orDefault(request.priceMax(), type.defaultPriceMax);

    if (distanceMinMi > distanceMaxMi) {
      throw ApiException.badRequest("bad_distance", "Minimal distance is greater than maximal");
    }
    if (priceMin > priceMax) {
      throw ApiException.badRequest("bad_price", "Minimal price is greater than maximal");
    }

    String timeZone = isBlank(request.timeZone()) ? null : request.timeZone().trim();
    if (timeZone != null && !isKnownTimeZone(timeZone)) {
      throw ApiException.badRequest("bad_time_zone", "Unknown time zone: " + timeZone);
    }

    // Nobody invited by name is fine: people then come by the link.
    Set<Long> inviteeIds = new LinkedHashSet<>(orEmpty(request.inviteeIds()));
    if (inviteeIds.contains(me)) {
      throw ApiException.badRequest("invite_self", "You cannot invite yourself");
    }
    if (!inviteeIds.isEmpty()
        && friendshipRepository.countFriendsAmong(me, inviteeIds) != inviteeIds.size()) {
      throw ApiException.badRequest("invitee_not_friend", "You can invite only your friends");
    }

    List<Long> chatMembers = new ArrayList<>();
    chatMembers.add(me);
    chatMembers.addAll(inviteeIds);
    Long chatId = chatService.createGroupChat(title, chatMembers);

    Instant now = Instant.now();

    Event event = new Event();
    event.creatorUserAccountId = me;
    event.chatId = chatId;
    event.shareToken = newShareToken();
    event.timeZone = timeZone;
    event.eventType = type;
    event.title = title;
    event.description = request.description();
    event.distanceMinMi = distanceMinMi;
    event.distanceMaxMi = distanceMaxMi;
    event.priceMin = priceMin;
    event.priceMax = priceMax;
    event.status = EventStatus.ACTIVE;
    event.createdAt = now;
    event = eventRepository.save(event);

    // A date given twice is one date.
    for (Instant startsAt : new TreeSet<>(orEmpty(request.proposedTimes()))) {
      EventTimeOption option = new EventTimeOption();
      option.eventId = event.id;
      option.startsAt = startsAt;
      eventTimeOptionRepository.save(option);
    }

    for (Long inviteeId : inviteeIds) {
      Invitation invitation = new Invitation();
      invitation.eventId = event.id;
      invitation.inviteeUserAccountId = inviteeId;
      invitation.status = InvitationStatus.PENDING;
      invitation.createdAt = now;
      invitationRepository.save(invitation);
    }

    return event.id;
  }

  /**
   * Accepting by the link: the caller becomes a guest who said yes and enters the chat of the
   * event. Works for someone never invited by name, for an invitee who has not answered, and for
   * one who declined and changed their mind. Calling it again changes nothing.
   *
   * @return id of the event
   */
  @Transactional
  public Long joinByLink(Long me, String shareToken) {
    Event event = eventRepository.findByShareToken(shareToken)
        .orElseThrow(() -> ApiException.notFound("event_not_found", "Event not found"));

    requireActive(event);
    if (event.creatorUserAccountId.equals(me)) {
      return event.id; // the creator is in already
    }

    Instant now = Instant.now();

    Invitation invitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(event.id, me).orElse(null);

    if (invitation == null) {
      invitation = new Invitation();
      invitation.eventId = event.id;
      invitation.inviteeUserAccountId = me;
      invitation.status = InvitationStatus.ACCEPTED;
      invitation.createdAt = now;
      invitation.respondedAt = now;
      invitationRepository.save(invitation);
    } else if (invitation.status != InvitationStatus.ACCEPTED) {
      invitation.status = InvitationStatus.ACCEPTED;
      invitation.respondedAt = now;
    }

    chatService.join(event.chatId, me);

    return event.id;
  }

  /**
   * A participant says where they are and how far they are ready to travel, for this event. Saving
   * again replaces what was said before.
   *
   * <p>For the creator and for a guest who has not declined: suggesting a place needs to know where
   * everyone is coming from, the creator included. Someone who is not coming has nothing to say.
   *
   * <p>The first time the location of everyone coming is known, places are suggested by themselves
   * (see {@link SuggestionService#suggestIfEveryoneIsKnown}).
   */
  @Transactional
  public void savePreference(Long me, Long eventId, SavePreferenceRequest request) {
    Event event = eventRepository.findById(eventId).orElse(null);
    boolean isCreator = event != null && event.creatorUserAccountId.equals(me);

    if (!isCreator) {
      Invitation invitation =
          invitationRepository.findByEventIdAndInviteeUserAccountId(eventId, me).orElse(null);

      // Not a part of it: the event "does not exist" for the caller.
      if (invitation == null) {
        throw ApiException.notFound("event_not_found", "Event not found");
      }
      if (invitation.status == InvitationStatus.DECLINED) {
        throw ApiException.conflict("invitation_declined", "You declined this invitation");
      }
    }

    boolean hasLatitude = request.latitude() != null;
    boolean hasLongitude = request.longitude() != null;
    String locationText = isBlank(request.locationText()) ? null : request.locationText().trim();

    if (hasLatitude != hasLongitude) {
      throw ApiException.badRequest("bad_location", "Latitude and longitude go together");
    }
    if (!hasLatitude && locationText == null) {
      throw ApiException.badRequest("no_location", "Share your location or type where you are");
    }

    ParticipantPreference preference = participantPreferenceRepository
        .findByEventIdAndUserAccountId(eventId, me)
        .orElseGet(() -> {
          ParticipantPreference created = new ParticipantPreference();
          created.eventId = eventId;
          created.userAccountId = me;
          return created;
        });

    preference.latitude = request.latitude();
    preference.longitude = request.longitude();
    preference.locationText = locationText;
    preference.travelRadiusMi = request.travelRadiusMi();
    preference.updatedAt = Instant.now();

    // Written to the database at once: the suggestions below are computed by a query, and a query
    // sees only what has been written.
    participantPreferenceRepository.saveAndFlush(preference);

    suggestionService.suggestIfEveryoneIsKnown(eventId);
  }

  /** Only the creator cancels. The chat stays. */
  @Transactional
  public void cancel(Long me, Long eventId) {
    Event event = eventRepository.findById(eventId).orElse(null);

    if (event == null || !event.creatorUserAccountId.equals(me)) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }
    requireActive(event);

    event.status = EventStatus.CANCELLED;
  }

  /**
   * Everything that changes an event happens only while it is ACTIVE. The refusal says which of the
   * two ends it has met.
   */
  static void requireActive(Event event) {
    switch (event.status) {
      case ACTIVE -> { }
      case CANCELLED -> throw ApiException.conflict("event_cancelled", "The event is cancelled");
      case EXPIRED -> throw ApiException.conflict("event_expired", "The event is over");
    }
  }

  @Transactional
  public void accept(Long me, Long invitationId) {
    answer(me, invitationId, InvitationStatus.ACCEPTED);
  }

  /** The one who declines also leaves the chat of the event. */
  @Transactional
  public void decline(Long me, Long invitationId) {
    Event event = answer(me, invitationId, InvitationStatus.DECLINED);
    chatService.leave(event.chatId, me);
  }

  private Event answer(Long me, Long invitationId, InvitationStatus answer) {
    Invitation invitation = invitationRepository.findById(invitationId).orElse(null);

    // Only the invitee answers. For anyone else the invitation "does not exist".
    if (invitation == null || !invitation.inviteeUserAccountId.equals(me)) {
      throw ApiException.notFound("invitation_not_found", "Invitation not found");
    }
    if (invitation.status != InvitationStatus.PENDING) {
      throw ApiException.conflict("invitation_answered", "This invitation is already answered");
    }

    Event event = eventRepository.findById(invitation.eventId).orElseThrow();
    requireActive(event);

    invitation.status = answer;
    invitation.respondedAt = Instant.now();

    return event;
  }

  private static String newShareToken() {
    byte[] bytes = new byte[SHARE_TOKEN_BYTES];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static boolean isKnownTimeZone(String name) {
    try {
      java.time.ZoneId.of(name);
      return true;
    } catch (java.time.DateTimeException e) {
      return false;
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static int orDefault(Integer value, int defaultValue) {
    return value == null ? defaultValue : value;
  }

  private static <T> List<T> orEmpty(List<T> list) {
    return list == null ? List.of() : list;
  }
}
