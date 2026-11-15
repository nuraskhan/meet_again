package kz.nuraskhan.hangelo.analytics;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import kz.nuraskhan.hangelo.analytics.dto.AnalyticsDto;
import kz.nuraskhan.hangelo.analytics.dto.VisitRequest;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {

  private static final int MAX_DAYS = 90;

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  /**
   * The page tells that it was opened. Open to everyone - visitors are counted too - so it lives
   * under /public. Someone signed in sends their token as with any request, and is counted as
   * signed in.
   */
  @PostMapping("/public/visit")
  public void visit(Principal principal, @Valid @RequestBody VisitRequest request) {
    analyticsService.visit(request.visitorId(), request.path(),
        principal == null ? null : CurrentUser.id(principal));
  }

  /** May the caller see the analytics: the page asks before showing its link. */
  @GetMapping("/admin/access")
  public Map<String, Boolean> access(Principal principal) {
    return Map.of("admin", analyticsService.isAdmin(CurrentUser.id(principal)));
  }

  /** The figures of the last so many days (30 unless said, 90 at most). Admins only. */
  @GetMapping("/admin/analytics")
  public AnalyticsDto analytics(Principal principal, @RequestParam(name = "days", defaultValue = "30") int days) {
    analyticsService.requireAdmin(CurrentUser.id(principal));
    return analyticsService.figures(Math.clamp(days, 1, MAX_DAYS));
  }
}
