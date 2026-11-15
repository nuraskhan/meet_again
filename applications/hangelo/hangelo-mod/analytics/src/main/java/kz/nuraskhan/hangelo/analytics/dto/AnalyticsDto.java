package kz.nuraskhan.hangelo.analytics.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The figures of the analytics page, for the last so many days.
 *
 * <p>A day is a UTC day.
 *
 * @param days            how many days the figures cover, today included
 * @param visits          pages opened
 * @param visitors        different browsers that opened them
 * @param signedInVisits  of the visits, how many were by someone signed in
 * @param users           accounts there are in all
 * @param newUsers        accounts created in these days
 * @param plans           plans created in these days
 * @param byDay           every day of the stretch, earliest first, days without visits included
 * @param topPages        the pages opened most, the most opened first
 */
public record AnalyticsDto(
    int days,
    long visits,
    long visitors,
    long signedInVisits,
    long users,
    long newUsers,
    long plans,
    List<Day> byDay,
    List<Page> topPages
) {

  public record Day(LocalDate day, long visits, long visitors) {
  }

  public record Page(String path, long visits, long visitors) {
  }
}
