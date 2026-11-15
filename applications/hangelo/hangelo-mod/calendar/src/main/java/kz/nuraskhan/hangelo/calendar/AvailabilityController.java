package kz.nuraskhan.hangelo.calendar;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import kz.nuraskhan.hangelo.calendar.dto.AvailabilityDto;
import kz.nuraskhan.hangelo.calendar.dto.FriendAvailabilityDto;
import kz.nuraskhan.hangelo.calendar.dto.SaveAvailabilityRequest;
import kz.nuraskhan.hangelo.web.ApiException;
import kz.nuraskhan.hangelo.web.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The calendar of availability: days a person marks as busy, free, or away in another city.
 *
 * <p>Days are plain dates ("2030-01-05"): no time and no time zone.
 */
@RestController
@RequestMapping("/availability")
public class AvailabilityController {

  /** The widest window one request may ask for: a little over a year. */
  private static final long MAX_WINDOW_DAYS = 400;

  private final AvailabilityService availabilityService;
  private final AvailabilityQuery availabilityQuery;

  public AvailabilityController(AvailabilityService availabilityService,
                                AvailabilityQuery availabilityQuery) {
    this.availabilityService = availabilityService;
    this.availabilityQuery = availabilityQuery;
  }

  /** The caller's own marks that touch the days from..to (both included). */
  @GetMapping("/list")
  public List<AvailabilityDto> list(Principal principal,
                                    @RequestParam("from") LocalDate from,
                                    @RequestParam("to") LocalDate to) {
    requireWindow(from, to);
    return availabilityQuery.mine(CurrentUser.id(principal), from, to);
  }

  /**
   * What the caller's friends marked for the days from..to - without their notes.
   *
   * @param userId only this friend; someone who is not the caller's friend has nothing here
   */
  @GetMapping("/friends")
  public List<FriendAvailabilityDto> friends(Principal principal,
                                             @RequestParam("from") LocalDate from,
                                             @RequestParam("to") LocalDate to,
                                             @RequestParam(name = "userId", required = false) Long userId) {
    requireWindow(from, to);
    return availabilityQuery.ofFriends(CurrentUser.id(principal), from, to, userId);
  }

  @PostMapping("/add")
  public AvailabilityDto add(Principal principal, @Valid @RequestBody SaveAvailabilityRequest request) {
    Long id = availabilityService.add(CurrentUser.id(principal), request);
    return availabilityQuery.one(id).orElseThrow();
  }

  @PostMapping("/{availabilityId}/remove")
  public void remove(Principal principal, @PathVariable("availabilityId") Long availabilityId) {
    availabilityService.remove(CurrentUser.id(principal), availabilityId);
  }

  private static void requireWindow(LocalDate from, LocalDate to) {
    if (to.isBefore(from) || from.plusDays(MAX_WINDOW_DAYS).isBefore(to)) {
      throw ApiException.badRequest("bad_window", "Ask for a year of days at most, first day first");
    }
  }
}
