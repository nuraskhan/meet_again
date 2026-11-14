package kz.nuraskhan.meet_again.magain.event;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import kz.nuraskhan.meet_again.magain.event.dto.CreateEventRequest;
import kz.nuraskhan.meet_again.magain.event.dto.SavePreferenceRequest;
import kz.nuraskhan.meet_again.magain.event.dto.EventDto;
import kz.nuraskhan.meet_again.magain.event.dto.EventTypeDto;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.web.ApiException;
import kz.nuraskhan.meet_again.web.CurrentUser;
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
  public EventDto refreshSuggestions(Principal principal, @PathVariable("eventId") Long eventId) {
    Long me = CurrentUser.id(principal);
    suggestionService.refresh(me, eventId);
    return eventQuery.get(me, eventId).orElseThrow();
  }

  @PostMapping("/{eventId}/cancel")
  public void cancel(Principal principal, @PathVariable("eventId") Long eventId) {
    eventService.cancel(CurrentUser.id(principal), eventId);
  }
}
