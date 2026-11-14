package kz.nuraskhan.hangelo.event;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import kz.nuraskhan.hangelo.chat.ChatService;
import kz.nuraskhan.hangelo.event.dto.CreateEventRequest;
import kz.nuraskhan.hangelo.notification.NotificationService;
import kz.nuraskhan.hangelo.event.dto.SavePreferenceRequest;
import kz.nuraskhan.hangelo.model.entity.Event;
import kz.nuraskhan.hangelo.model.entity.EventTimeOption;
import kz.nuraskhan.hangelo.model.entity.Invitation;
import kz.nuraskhan.hangelo.model.entity.ParticipantPreference;
import kz.nuraskhan.hangelo.model.enums.EventStatus;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.enums.EventVisibility;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import kz.nuraskhan.hangelo.model.enums.NotificationKind;
import kz.nuraskhan.hangelo.model.repository.EventRepository;
import kz.nuraskhan.hangelo.model.repository.EventTimeOptionRepository;
import kz.nuraskhan.hangelo.model.repository.FriendshipRepository;
import kz.nuraskhan.hangelo.model.repository.InvitationRepository;
import kz.nuraskhan.hangelo.model.repository.ParticipantPreferenceRepository;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  /** 18 random bytes = 144 bits: the link cannot be guessed. In base64url that is 24 characters. */
  private static final int SHARE_TOKEN_BYTES = 18;

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * The most guests an event holds - invited and not yet answered, or coming. The same number
   * limits how many people can be invited by name at once (CreateEventRequest.inviteeIds). It keeps
   * a public event, and its chat, from growing without end.
   */
  static final int MAX_GUESTS = 50;

  private final EventRepository eventRepository;
  private final EventTimeOptionRepository eventTimeOptionRepository;
  private final InvitationRepository invitationRepository;
  private final FriendshipRepository friendshipRepository;
  private final ParticipantPreferenceRepository participantPreferenceRepository;
  private final ChatService chatService;
  private final SuggestionService suggestionService;
  private final NotificationService notificationService;

  public EventService(EventRepository eventRepository,
                      EventTimeOptionRepository eventTimeOptionRepository,
                      InvitationRepository invitationRepository,
                      FriendshipRepository friendshipRepository,
                      ParticipantPreferenceRepository participantPreferenceRepository,
                      ChatService chatService,
                      SuggestionService suggestionService,
                      NotificationService notificationService) {
    this.eventRepository = eventRepository;
    this.eventTimeOptionRepository = eventTimeOptionRepository;
    this.invitationRepository = invitationRepository;
    this.friendshipRepository = friendshipRepository;
    this.participantPreferenceRepository = participantPreferenceRepository;
    this.chatService = chatService;
    this.suggestionService = suggestionService;
    this.notificationService = notificationService;
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
    event.visibility = request.visibility() == null ? EventVisibility.PRIVATE : request.visibility();
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

      notificationService.tell(inviteeId, NotificationKind.EVENT_INVITATION, me, event.id);
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

    join(me, event);
    return event.id;
  }

  /**
   * Joining from the feed: the same as by the link, but needs no link - the event is public.
   * A private event "does not exist" here, whoever asks: its id is not an invitation.
   */
  @Transactional
  public void joinPublic(Long me, Long eventId) {
    Event event = eventRepository.findById(eventId).orElse(null);
    if (event == null || event.visibility != EventVisibility.PUBLIC) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }

    join(me, event);
  }

  /** The caller becomes a guest who said yes and enters the chat. Calling it again changes nothing. */
  private void join(Long me, Event event) {
    requireActive(event);
    if (event.creatorUserAccountId.equals(me)) {
      return; // the creator is in already
    }

    Instant now = Instant.now();

    Invitation invitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(event.id, me).orElse(null);

    if (invitation != null && invitation.status == InvitationStatus.ACCEPTED) {
      return;
    }
    // Shown the door by the creator: neither the feed nor the link lets them back in.
    if (invitation != null && invitation.status == InvitationStatus.REMOVED) {
      throw ApiException.conflict("removed_from_event", "You were removed from this event");
    }
    // Someone invited by name and not answered yet already has a place; anyone else needs one.
    boolean takesNewPlace = invitation == null || invitation.status == InvitationStatus.DECLINED;
    if (takesNewPlace && invitationRepository.countGuests(event.id) >= MAX_GUESTS) {
      throw ApiException.conflict("event_full", "This event is full");
    }

    if (invitation == null) {
      invitation = new Invitation();
      invitation.eventId = event.id;
      invitation.inviteeUserAccountId = me;
      invitation.createdAt = now;
    }
    invitation.status = InvitationStatus.ACCEPTED;
    invitation.respondedAt = now;
    invitationRepository.save(invitation);

    chatService.join(event.chatId, me);

    notificationService.tell(event.creatorUserAccountId, NotificationKind.GUEST_JOINED, me, event.id);
  }

  /**
   * A guest who said yes changes their mind: they are no longer coming and leave the chat. They
   * may join again later. (Someone who has not answered yet declines instead.)
   */
  @Transactional
  public void leave(Long me, Long eventId) {
    Invitation invitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(eventId, me).orElse(null);

    if (invitation == null || invitation.status != InvitationStatus.ACCEPTED) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }

    Event event = eventRepository.findById(eventId).orElseThrow();
    requireActive(event);

    invitation.status = InvitationStatus.DECLINED;
    invitation.respondedAt = Instant.now();
    chatService.leave(event.chatId, me);

    notificationService.tell(event.creatorUserAccountId, NotificationKind.GUEST_LEFT, me, event.id);
  }

  /**
   * The creator takes a guest out of the event and of its chat. Unlike a guest who left, a removed
   * one cannot join again - this is the creator's only tool against an unwanted person in an event
   * anyone can join.
   */
  @Transactional
  public void removeGuest(Long me, Long eventId, Long guestId) {
    Event event = eventRepository.findById(eventId).orElse(null);
    if (event == null || !event.creatorUserAccountId.equals(me)) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }
    requireActive(event);

    Invitation invitation =
        invitationRepository.findByEventIdAndInviteeUserAccountId(eventId, guestId).orElse(null);
    if (invitation == null || !invitation.status.isIn()) {
      throw ApiException.notFound("guest_not_found", "This person is not a guest of the event");
    }

    invitation.status = InvitationStatus.REMOVED;
    invitation.respondedAt = Instant.now();
    chatService.leave(event.chatId, guestId);

    notificationService.tell(guestId, NotificationKind.REMOVED_FROM_EVENT, me, event.id);
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
      if (!invitation.status.isIn()) {
        throw ApiException.conflict("invitation_declined", "You are not part of this event any more");
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

    // Everyone who is part of it - invited and not answered, or coming - learns that it is off.
    for (Long guestId : invitationRepository.findGuestIds(eventId)) {
      notificationService.tell(guestId, NotificationKind.EVENT_CANCELLED, me, eventId);
    }
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

    notificationService.tell(event.creatorUserAccountId,
        answer == InvitationStatus.ACCEPTED
            ? NotificationKind.INVITATION_ACCEPTED
            : NotificationKind.INVITATION_DECLINED,
        me, event.id);

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
