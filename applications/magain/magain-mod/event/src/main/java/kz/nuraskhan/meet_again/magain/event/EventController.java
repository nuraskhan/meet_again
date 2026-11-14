package kz.nuraskhan.meet_again.magain.event;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import kz.nuraskhan.meet_again.magain.event.dto.CreateEventRequest;
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

  public EventController(EventService eventService, EventQuery eventQuery) {
    this.eventService = eventService;
    this.eventQuery = eventQuery;
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

  @GetMapping("/list")
  public List<EventDto> list(Principal principal) {
    return eventQuery.list(CurrentUser.id(principal));
  }

  @GetMapping("/{eventId}")
  public EventDto get(Principal principal, @PathVariable("eventId") Long eventId) {
    return eventQuery.get(CurrentUser.id(principal), eventId)
        .orElseThrow(() -> ApiException.notFound("event_not_found", "Event not found"));
  }

  @PostMapping("/{eventId}/cancel")
  public void cancel(Principal principal, @PathVariable("eventId") Long eventId) {
    eventService.cancel(CurrentUser.id(principal), eventId);
  }
}
