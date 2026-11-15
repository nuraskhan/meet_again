package kz.nuraskhan.hangelo.event;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.event.dto.CreateEventRequest;
import kz.nuraskhan.hangelo.event.dto.SavePreferenceRequest;
import kz.nuraskhan.hangelo.event.dto.SuggestRequest;
import kz.nuraskhan.hangelo.event.dto.EventDto;
import kz.nuraskhan.hangelo.event.dto.EventTypeDto;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.web.ApiException;
import kz.nuraskhan.hangelo.model.enums.InvitationStatus;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/event")
public class EventController {

  private final EventService eventService;
  private final EventQuery eventQuery;
  private final SuggestionService suggestionService;

  public EventController(EventService eventService, EventQuery eventQuery,
                         SuggestionService suggestionService) {
    this.eventService = eventService;
    this.eventQuery = eventQuery;
    this.suggestionService = suggestionService;
  }

  /** What the invitation form offers: the types, each with its pre-filled values. */
  @GetMapping("/type/list")
  public List<EventTypeDto> types() {
    return Arrays.stream(EventType.values()).map(EventTypeDto::of).toList();
  }

  @PostMapping("/create")
  public EventDto create(Principal principal, @Valid @RequestBody CreateEventRequest request) {
    Long me = CurrentUser.id(principal);
    Long eventId = eventService.create(me, request);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /**
   * Accepting by the link: the caller becomes a guest who said yes. Needs a token like everything
   * here - reading the invitation without one is {@code GET /public/event/{shareToken}}.
   */
  @PostMapping("/join/{shareToken}")
  public EventDto join(Principal principal, @PathVariable("shareToken") String shareToken) {
    Long me = CurrentUser.id(principal);
    Long eventId = eventService.joinByLink(me, shareToken);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /**
   * Asking to join a public event from the feed. The answer says where the caller stands now:
   * REQUESTED - the request waits for the creator; ACCEPTED - they were invited by name (or are
   * in already) and are coming; CREATOR - it is their own event. For a private event this answers
   * "not found" - its id is not an invitation.
   *
   * <p>The event itself is not returned: someone who only asked does not see it yet.
   */
  @PostMapping("/{eventId}/join")
  public Map<String, String> joinPublic(Principal principal, @PathVariable("eventId") Long eventId) {
    InvitationStatus standing = eventService.joinPublic(CurrentUser.id(principal), eventId);
    return Map.of("standing", standing == null ? "CREATOR" : standing.name());
  }

  /** The creator lets in someone who asked to join. */
  @PostMapping("/{eventId}/request/{userId}/approve")
  public EventDto approveRequest(Principal principal,
                                 @PathVariable("eventId") Long eventId,
                                 @PathVariable("userId") Long userId) {
    Long me = CurrentUser.id(principal);
    eventService.approveRequest(me, eventId, userId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** The creator says no to someone who asked to join. They may ask again. */
  @PostMapping("/{eventId}/request/{userId}/decline")
  public EventDto declineRequest(Principal principal,
                                 @PathVariable("eventId") Long eventId,
                                 @PathVariable("userId") Long userId) {
    Long me = CurrentUser.id(principal);
    eventService.declineRequest(me, eventId, userId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /**
   * A guest who said yes is not coming after all, and leaves the chat. They may join again.
   * Someone whose request to join is still waiting takes it back with the same call. A guest of
   * an event that is over or cancelled takes it, and its chat, off their own lists.
   */
  @PostMapping("/{eventId}/leave")
  public void leave(Principal principal, @PathVariable("eventId") Long eventId) {
    eventService.leave(CurrentUser.id(principal), eventId);
  }

  /** The creator takes a guest out of the event and its chat. That person cannot join again. */
  @PostMapping("/{eventId}/guest/{userId}/remove")
  public EventDto removeGuest(Principal principal,
                              @PathVariable("eventId") Long eventId,
                              @PathVariable("userId") Long userId) {
    Long me = CurrentUser.id(principal);
    eventService.removeGuest(me, eventId, userId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  @GetMapping("/list")
  public List<EventDto> list(Principal principal) {
    return eventQuery.list(CurrentUser.id(principal));
  }

  @GetMapping("/{eventId}")
  public EventDto get(Principal principal, @PathVariable("eventId") Long eventId) {
    return eventQuery.get(CurrentUser.id(principal), eventId)
        .orElseThrow(() -> ApiException.notFound("event_not_found", "Event not found"));
  }

  /** A guest says where they are and how far they travel, for this event. */
  @PostMapping("/{eventId}/preference")
  public EventDto savePreference(Principal principal,
                                 @PathVariable("eventId") Long eventId,
                                 @Valid @RequestBody SavePreferenceRequest request) {
    Long me = CurrentUser.id(principal);
    eventService.savePreference(me, eventId, request);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /**
   * "Suggest places": looks for the best places for the people coming, and keeps the answer with
   * the event. Every later reading of the event shows what was kept; calling this again replaces it.
   */
  @PostMapping("/{eventId}/suggestion/refresh")
  public EventDto refreshSuggestions(Principal principal,
                                     @PathVariable("eventId") Long eventId,
                                     @Valid @RequestBody(required = false) SuggestRequest request) {
    Long me = CurrentUser.id(principal);
    // With the caller's position in the body, one press is enough for someone who has not said
    // where they are yet: see EventService.suggest.
    eventService.suggest(me, eventId, request);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** The creator chooses the venue of the event (or changes it). */
  @PostMapping("/{eventId}/place/{placeId}/attach")
  public EventDto attachPlace(Principal principal,
                              @PathVariable("eventId") Long eventId,
                              @PathVariable("placeId") Long placeId) {
    Long me = CurrentUser.id(principal);
    eventService.attachPlace(me, eventId, placeId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** The creator takes the venue off the event. */
  @PostMapping("/{eventId}/place/detach")
  public EventDto detachPlace(Principal principal, @PathVariable("eventId") Long eventId) {
    Long me = CurrentUser.id(principal);
    eventService.detachPlace(me, eventId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** After the event the creator marks a guest who said "coming" and did not come. */
  @PostMapping("/{eventId}/guest/{userId}/no-show")
  public EventDto noShow(Principal principal,
                         @PathVariable("eventId") Long eventId,
                         @PathVariable("userId") Long userId) {
    Long me = CurrentUser.id(principal);
    eventService.markNoShow(me, eventId, userId, true);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** The creator takes the "did not come" mark back. */
  @PostMapping("/{eventId}/guest/{userId}/came")
  public EventDto came(Principal principal,
                       @PathVariable("eventId") Long eventId,
                       @PathVariable("userId") Long userId) {
    Long me = CurrentUser.id(principal);
    eventService.markNoShow(me, eventId, userId, false);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  /** The creator deletes the event: it and its chat disappear for everyone. */
  @PostMapping("/{eventId}/delete")
  public void delete(Principal principal, @PathVariable("eventId") Long eventId) {
    eventService.delete(CurrentUser.id(principal), eventId);
  }

  @PostMapping("/{eventId}/cancel")
  public void cancel(Principal principal, @PathVariable("eventId") Long eventId) {
    eventService.cancel(CurrentUser.id(principal), eventId);
  }
}
