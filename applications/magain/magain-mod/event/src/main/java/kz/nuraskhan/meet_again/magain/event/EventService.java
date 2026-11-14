package kz.nuraskhan.meet_again.magain.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kz.nuraskhan.meet_again.magain.chat.ChatService;
import kz.nuraskhan.meet_again.magain.event.dto.CreateEventRequest;
import kz.nuraskhan.meet_again.model.entity.Event;
import kz.nuraskhan.meet_again.model.entity.Invitation;
import kz.nuraskhan.meet_again.model.enums.EventStatus;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.model.enums.InvitationStatus;
import kz.nuraskhan.meet_again.model.repository.EventRepository;
import kz.nuraskhan.meet_again.model.repository.FriendshipRepository;
import kz.nuraskhan.meet_again.model.repository.InvitationRepository;
import kz.nuraskhan.meet_again.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private final EventRepository eventRepository;
  private final InvitationRepository invitationRepository;
  private final FriendshipRepository friendshipRepository;
  private final ChatService chatService;

  public EventService(EventRepository eventRepository,
                      InvitationRepository invitationRepository,
                      FriendshipRepository friendshipRepository,
                      ChatService chatService) {
    this.eventRepository = eventRepository;
    this.invitationRepository = invitationRepository;
    this.friendshipRepository = friendshipRepository;
    this.chatService = chatService;
  }

  /**
   * Creates the event, an invitation for every invitee, and the group chat with the creator and all
   * the invitees in it - all or nothing.
   *
   * @return id of the event
   */
  @Transactional
  public Long create(Long me, CreateEventRequest request) {
    EventType type = request.eventType();

    // What the form leaves out comes from the type: that is the "pre-filled" part.
    String title = isBlank(request.title()) ? type.defaultTitle : request.title().trim();
    int distanceMinKm = orDefault(request.distanceMinKm(), type.defaultDistanceMinKm);
    int distanceMaxKm = orDefault(request.distanceMaxKm(), type.defaultDistanceMaxKm);
    int priceMin = orDefault(request.priceMin(), type.defaultPriceMin);
    int priceMax = orDefault(request.priceMax(), type.defaultPriceMax);

    if (distanceMinKm > distanceMaxKm) {
      throw ApiException.badRequest("bad_distance", "Minimal distance is greater than maximal");
    }
    if (priceMin > priceMax) {
      throw ApiException.badRequest("bad_price", "Minimal price is greater than maximal");
    }

    Set<Long> inviteeIds = new LinkedHashSet<>(request.inviteeIds());
    if (inviteeIds.contains(me)) {
      throw ApiException.badRequest("invite_self", "You cannot invite yourself");
    }
    if (friendshipRepository.countFriendsAmong(me, inviteeIds) != inviteeIds.size()) {
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
    event.eventType = type;
    event.title = title;
    event.description = request.description();
    event.distanceMinKm = distanceMinKm;
    event.distanceMaxKm = distanceMaxKm;
    event.priceMin = priceMin;
    event.priceMax = priceMax;
    event.startsAt = request.startsAt();
    event.status = EventStatus.ACTIVE;
    event.createdAt = now;
    event = eventRepository.save(event);

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

  /** Only the creator cancels. The chat stays. */
  @Transactional
  public void cancel(Long me, Long eventId) {
    Event event = eventRepository.findById(eventId).orElse(null);

    if (event == null || !event.creatorUserAccountId.equals(me)) {
      throw ApiException.notFound("event_not_found", "Event not found");
    }
    if (event.status != EventStatus.ACTIVE) {
      throw ApiException.conflict("event_cancelled", "The event is already cancelled");
    }

    event.status = EventStatus.CANCELLED;
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
    if (event.status != EventStatus.ACTIVE) {
      throw ApiException.conflict("event_cancelled", "The event is cancelled");
    }

    invitation.status = answer;
    invitation.respondedAt = Instant.now();

    return event;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static int orDefault(Integer value, int defaultValue) {
    return value == null ? defaultValue : value;
  }
}
