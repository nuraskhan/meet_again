package kz.nuraskhan.hangelo.event;

import kz.nuraskhan.hangelo.event.dto.PublicEventDto;
import kz.nuraskhan.hangelo.web.ApiException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What is open WITHOUT a token: everything under {@code /public} (see SecurityConfig in the auth
 * module). Only reading: to accept, a person registers and calls {@code POST /event/join/...}.
 */
@RestController
@RequestMapping("/public")
public class PublicEventController {

  private final EventQuery eventQuery;

  public PublicEventController(EventQuery eventQuery) {
    this.eventQuery = eventQuery;
  }

  /** The invitation a link leads to. The token in the link is the only protection, by design. */
  @GetMapping("/event/{shareToken}")
  public PublicEventDto event(@PathVariable("shareToken") String shareToken) {
    return eventQuery.byShareToken(shareToken)
        .orElseThrow(() -> ApiException.notFound("event_not_found", "Event not found"));
  }
}
