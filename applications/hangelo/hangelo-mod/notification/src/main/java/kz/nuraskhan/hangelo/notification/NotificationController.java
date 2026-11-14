package kz.nuraskhan.hangelo.notification;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.notification.dto.NotificationDto;
import kz.nuraskhan.hangelo.web.ApiException;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The caller's own notifications. New ones also arrive by themselves: see SignalController. */
@RestController
@RequestMapping("/notification")
public class NotificationController {

  private static final int MAX_PAGE = 50;

  private final NotificationService notificationService;
  private final NotificationQuery notificationQuery;

  public NotificationController(NotificationService notificationService, NotificationQuery notificationQuery) {
    this.notificationService = notificationService;
    this.notificationQuery = notificationQuery;
  }

  /** Newest first; the next page is asked with {@code beforeId} = id of the last one got. */
  @GetMapping("/list")
  public List<NotificationDto> list(
      Principal principal,
      @RequestParam(name = "beforeId", required = false) Long beforeId,
      @RequestParam(name = "limit", defaultValue = "20") int limit) {
    return notificationQuery.page(CurrentUser.id(principal), beforeId, Math.clamp(limit, 1, MAX_PAGE));
  }

  @GetMapping("/unread-count")
  public Map<String, Integer> unreadCount(Principal principal) {
    return Map.of("count", notificationQuery.unreadCount(CurrentUser.id(principal)));
  }

  /** Marks one as read. Reading an already read one is fine; somebody else's is "not found". */
  @PostMapping("/{notificationId}/read")
  public Map<String, Integer> read(Principal principal, @PathVariable("notificationId") Long notificationId) {
    Long me = CurrentUser.id(principal);

    boolean marked = notificationService.markRead(me, notificationId);
    if (!marked && !notificationQuery.isMine(me, notificationId)) {
      throw ApiException.notFound("notification_not_found", "Notification not found");
    }
    return Map.of("count", notificationQuery.unreadCount(me));
  }

  @PostMapping("/read-all")
  public Map<String, Integer> readAll(Principal principal) {
    notificationService.markAllRead(CurrentUser.id(principal));
    return Map.of("count", 0);
  }
}
