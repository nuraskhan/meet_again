package kz.nuraskhan.hangelo.event;

import java.security.Principal;
import java.util.List;
import kz.nuraskhan.hangelo.event.dto.PlaceDto;
import kz.nuraskhan.hangelo.model.enums.EventType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Venues: the places an event can be held at. Read-only here - venues are loaded by the import
 * tool, not entered by people. Choosing one for an event is in EventController.
 */
@RestController
@RequestMapping("/place")
public class PlaceController {

  private static final int MAX_PAGE = 50;

  private final EventQuery eventQuery;

  public PlaceController(EventQuery eventQuery) {
    this.eventQuery = eventQuery;
  }

  /**
   * Venues to choose from, the best rated first.
   *
   * @param category only of this category; absent = any
   * @param city     only in this city; absent = any
   * @param q        a part of the name or the address; absent = any
   */
  @GetMapping("/list")
  public List<PlaceDto> list(Principal principal,
                             @RequestParam(name = "category", required = false) EventType category,
                             @RequestParam(name = "city", required = false) String city,
                             @RequestParam(name = "q", required = false) String q,
                             @RequestParam(name = "limit", defaultValue = "20") int limit) {
    return eventQuery.searchPlaces(category, city, q, Math.clamp(limit, 1, MAX_PAGE));
  }

  /** The cities there are venues in, the one with the most venues first. */
  @GetMapping("/city/list")
  public List<String> cities(Principal principal) {
    return eventQuery.placeCities();
  }
}
