package kz.nuraskhan.hangelo.event;

import java.security.Principal;
import java.util.List;
import kz.nuraskhan.hangelo.event.dto.FeedItemDto;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The feed: public events anyone logged in can look through and join. Joining itself is
 * {@code POST /event/{id}/join} (EventController).
 */
@RestController
@RequestMapping("/feed")
public class FeedController {

  private static final int MAX_PAGE = 50;

  private final FeedQuery feedQuery;

  public FeedController(FeedQuery feedQuery) {
    this.feedQuery = feedQuery;
  }

  /** Newest first; the next page is asked with {@code beforeId} = id of the last event got. */
  @GetMapping("/list")
  public List<FeedItemDto> list(
      Principal principal,
      @RequestParam(name = "eventType", required = false) EventType eventType,
      @RequestParam(name = "beforeId", required = false) Long beforeId,
      @RequestParam(name = "limit", defaultValue = "20") int limit) {
    return feedQuery.page(CurrentUser.id(principal), eventType, beforeId, Math.clamp(limit, 1, MAX_PAGE));
  }
}
