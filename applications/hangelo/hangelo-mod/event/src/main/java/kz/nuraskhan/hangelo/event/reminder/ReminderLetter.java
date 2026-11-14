package kz.nuraskhan.hangelo.event.reminder;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import kz.nuraskhan.hangelo.mail.Email;
import kz.nuraskhan.hangelo.mail.Letter;

/** The text of a reminder. No database and no clock here: everything it says is given to it. */
final class ReminderLetter {

  // US style, as everything shown to people: "Saturday, November 21 at 6:00 PM EST".
  private static final DateTimeFormatter LONG =
      DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a zzz", Locale.US);
  private static final DateTimeFormatter SHORT =
      DateTimeFormatter.ofPattern("EEE, MMM d, h:mm a", Locale.US);

  /** As a named zone, so the letter says "UTC" and not "Z". */
  private static final ZoneId UTC = ZoneId.of("UTC");

  private ReminderLetter() {}

  /**
   * @param timeZone     the IANA name saved with the event; null or unknown = UTC, and the letter
   *                     then says "UTC", so the time is never silently wrong
   * @param optionsAhead how many proposed dates are still ahead, this one included
   * @param planUrl      the address of the plan on the site
   */
  static Email build(String to, String name, String title, Instant startsAt, String timeZone,
                     int optionsAhead, String planUrl) {
    ZoneId zone = zoneOrUtc(timeZone);

    Letter letter = Letter.to(to)
        .subject("Coming up: " + title + " - " + SHORT.format(startsAt.atZone(zone)))
        .heading(title)
        .paragraph("Hi " + name + ", your plan is coming up: " + LONG.format(startsAt.atZone(zone)) + ".");

    if (optionsAhead > 1) {
      letter.paragraph("This is the first of " + optionsAhead
          + " proposed dates. If you have not settled on one yet, the chat is the place.");
    }

    return letter
        .button("Open the plan", planUrl)
        .footnote("You get this reminder because you are part of this plan on Hangelo.")
        .build();
  }

  static ZoneId zoneOrUtc(String timeZone) {
    if (timeZone == null || timeZone.isBlank()) {
      return UTC;
    }
    try {
      return ZoneId.of(timeZone);
    } catch (DateTimeException e) {
      return UTC;
    }
  }
}
