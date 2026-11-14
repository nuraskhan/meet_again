package kz.nuraskhan.hangelo.event.reminder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import kz.nuraskhan.hangelo.mail.Email;
import org.junit.jupiter.api.Test;

/**
 * The time in a reminder is shown in the time zone saved with the event, never in the zone of the
 * server. The expected texts are written out per zone, not computed.
 */
class ReminderLetterTest {

  // Tuesday morning in UTC; still Monday evening, the previous year, in America.
  private static final Instant STARTS_AT = Instant.parse("2030-01-01T02:30:00Z");

  @Test
  void build__showsTheTimeInTheZoneOfTheEvent_newYork() {
    //
    //
    Email actual = build("America/New_York", 1);
    //
    //

    assertThat(actual.subject()).isEqualTo("Coming up: Game night - Mon, Dec 31, 9:30 PM");
    assertThat(actual.text()).contains("Monday, December 31 at 9:30 PM EST");
  }

  @Test
  void build__showsTheTimeInTheZoneOfTheEvent_losAngeles() {
    //
    //
    Email actual = build("America/Los_Angeles", 1);
    //
    //

    assertThat(actual.subject()).isEqualTo("Coming up: Game night - Mon, Dec 31, 6:30 PM");
    assertThat(actual.text()).contains("Monday, December 31 at 6:30 PM PST");
  }

  @Test
  void build__saysUtc_whenTheZoneIsMissingOrUnknown() {
    //
    //
    Email withoutZone = build(null, 1);
    Email withUnknownZone = build("Mars/Olympus_Mons", 1);
    //
    //

    assertThat(withoutZone.text()).contains("Tuesday, January 1 at 2:30 AM UTC");
    assertThat(withUnknownZone.text()).contains("Tuesday, January 1 at 2:30 AM UTC");
  }

  @Test
  void build__mentionsOtherProposedDates_onlyWhenThereAreSome() {
    //
    //
    Email oneDate = build("America/New_York", 1);
    Email threeDates = build("America/New_York", 3);
    //
    //

    assertThat(oneDate.text()).doesNotContain("proposed dates");
    assertThat(threeDates.text()).contains("the first of 3 proposed dates");
  }

  @Test
  void build__linksToThePlan_andGreetsByName() {
    //
    //
    Email actual = build("America/New_York", 1);
    //
    //

    assertThat(actual.to()).isEqualTo("ann@example.test");
    assertThat(actual.text()).contains("Hi Ann,").contains("Open the plan: https://site.test/plans/7");
    assertThat(actual.html()).contains("href=\"https://site.test/plans/7\"");
  }

  private static Email build(String timeZone, int optionsAhead) {
    return ReminderLetter.build("ann@example.test", "Ann", "Game night", STARTS_AT, timeZone,
        optionsAhead, "https://site.test/plans/7");
  }
}
