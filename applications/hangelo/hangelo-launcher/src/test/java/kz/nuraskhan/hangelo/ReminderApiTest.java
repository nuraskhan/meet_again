package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.event.reminder.EventReminderJob;
import kz.nuraskhan.hangelo.mail.Email;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * Reminders about upcoming events. The timer is off in tests: each test runs the job itself, for
 * the moment it chooses.
 *
 * <p>The job looks at the whole database, so a run also "reminds" people of other tests. That is
 * harmless: every test looks only at the letters of the people it registered.
 */
class ReminderApiTest extends ApiTestBase {

  @Autowired CapturingEmailSender mail;
  @Autowired EventReminderJob job;

  @Test
  void sendDue__remindsTheCreatorAndTheGuestsWhoSaidYes() {
    TestUser creator = confirmed();
    TestUser saidYes = confirmed();
    TestUser notAnswered = confirmed();
    TestUser declined = confirmed();
    Instant now = Instant.now();
    JsonNode event = eventWithGuests(creator, now.plus(3, ChronoUnit.HOURS), saidYes, notAnswered, declined);
    answer(event, saidYes, "accept");
    answer(event, declined, "decline");

    //
    //
    job.sendDue(now);
    //
    //

    for (TestUser reminded : List.of(creator, saidYes)) {
      List<Email> letters = reminders(reminded);
      assertThat(letters).hasSize(1);
      assertThat(letters.get(0).subject()).startsWith("Coming up: Friday pizza - ");
      assertThat(letters.get(0).text())
          .contains("Hi " + reminded.displayName())
          .contains("Open the plan: https://site.test/plans/" + event.get("id").asLong());
    }
    assertThat(reminders(notAnswered)).isEmpty();
    assertThat(reminders(declined)).isEmpty();
  }

  @Test
  void sendDue__neverWritesToAnAddressThatIsNotConfirmed() {
    TestUser creator = register(); // registered, the link never opened
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(3, ChronoUnit.HOURS));

    //
    //
    job.sendDue(now);
    //
    //

    assertThat(reminders(creator)).isEmpty();
  }

  @Test
  void sendDue__remindsOnce_howeverOftenItRuns() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(3, ChronoUnit.HOURS));

    //
    //
    job.sendDue(now);
    job.sendDue(now.plus(10, ChronoUnit.MINUTES));
    job.sendDue(now.plus(2, ChronoUnit.HOURS));
    //
    //

    assertThat(reminders(creator)).hasSize(1);
  }

  @Test
  void sendDue__waitsUntilTheDateIsLessThanADayAway() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(30, ChronoUnit.HOURS));

    //
    //
    job.sendDue(now);
    int lettersWhenThirtyHoursAway = reminders(creator).size();
    job.sendDue(now.plus(7, ChronoUnit.HOURS));
    int lettersWhenTwentyThreeHoursAway = reminders(creator).size();
    //
    //

    assertThat(lettersWhenThirtyHoursAway).isZero();
    assertThat(lettersWhenTwentyThreeHoursAway).isEqualTo(1);
  }

  @Test
  void sendDue__ignoresADateAlreadyBehind_andRemindsAboutTheNextOne() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(2, ChronoUnit.HOURS), now.plus(50, ChronoUnit.HOURS));

    //
    //
    // Three hours later the first date is behind, and the second is still two days away.
    job.sendDue(now.plus(3, ChronoUnit.HOURS));
    int lettersAfterTheFirstDatePassed = reminders(creator).size();
    // A day before the second date.
    job.sendDue(now.plus(27, ChronoUnit.HOURS));
    //
    //

    assertThat(lettersAfterTheFirstDatePassed).isZero();
    assertThat(reminders(creator)).hasSize(1);
    assertThat(reminders(creator).get(0).text()).doesNotContain("proposed dates");
  }

  @Test
  void sendDue__saysThereAreOtherProposedDates() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(2, ChronoUnit.HOURS), now.plus(50, ChronoUnit.HOURS));

    //
    //
    job.sendDue(now);
    //
    //

    assertThat(reminders(creator)).hasSize(1);
    assertThat(reminders(creator).get(0).text()).contains("the first of 2 proposed dates");
  }

  @Test
  void sendDue__skipsACancelledEvent_andAnEventWithoutDates() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    JsonNode cancelled = eventWithDates(creator, now.plus(3, ChronoUnit.HOURS));
    post("/event/" + cancelled.get("id").asLong() + "/cancel", creator);
    eventWithDates(creator); // "time to be agreed"

    //
    //
    job.sendDue(now);
    //
    //

    assertThat(reminders(creator)).isEmpty();
  }

  @Test
  void sendDue__triesAgainNextTime_whenTheLetterCouldNotBeSent() {
    TestUser creator = confirmed();
    Instant now = Instant.now();
    eventWithDates(creator, now.plus(3, ChronoUnit.HOURS));

    //
    //
    mail.refuse(creator.email());
    try {
      job.sendDue(now);
    } finally {
      mail.refuseNobody();
    }
    int lettersWhileTheProviderWasDown = reminders(creator).size();
    job.sendDue(now.plus(10, ChronoUnit.MINUTES));
    //
    //

    assertThat(lettersWhileTheProviderWasDown).isZero();
    assertThat(reminders(creator)).hasSize(1);
  }

  @Test
  void create__refusesAnUnknownTimeZone() {
    TestUser creator = register();

    //
    //
    Response response = post("/event/create", creator,
        Map.of("eventType", "FOOD", "timeZone", "Mars/Olympus_Mons"));
    //
    //

    assertThat(response.status()).isEqualTo(400);
    assertThat(response.body().get("code").asString()).isEqualTo("bad_time_zone");
  }

  // ------------------------------------------------------------------ helpers

  /** A registered person who opened the link of the confirmation letter. */
  private TestUser confirmed() {
    TestUser user = register();
    Response verified = post("/auth/verify-email", null, Map.of("token", mail.verificationToken(user.email())));
    assertThat(verified.status()).isEqualTo(200);
    return user;
  }

  /** An event of {@code creator} with these proposed dates; no guests. */
  private JsonNode eventWithDates(TestUser creator, Instant... proposedTimes) {
    return event(creator, proposedTimes, new TestUser[0]);
  }

  /** An event of {@code creator} with one proposed date and these guests, invited as friends. */
  private JsonNode eventWithGuests(TestUser creator, Instant proposedTime, TestUser... guests) {
    return event(creator, new Instant[] {proposedTime}, guests);
  }

  private JsonNode event(TestUser creator, Instant[] proposedTimes, TestUser[] guests) {
    for (TestUser guest : guests) {
      makeFriends(creator, guest);
    }
    Response response = post("/event/create", creator, Map.of(
        "eventType", "FOOD",
        "title", "Friday pizza",
        "timeZone", "America/New_York",
        "proposedTimes", Arrays.stream(proposedTimes).map(Instant::toString).toList(),
        "inviteeIds", Arrays.stream(guests).map(TestUser::id).toList()));
    assertThat(response.status()).isEqualTo(200);
    return response.body();
  }

  /** @param answer "accept" or "decline" */
  private void answer(JsonNode event, TestUser guest, String answer) {
    long invitationId = event.get("invitees").valueStream()
        .filter(row -> row.get("userId").asLong() == guest.id())
        .findFirst().orElseThrow()
        .get("invitationId").asLong();
    assertThat(post("/invitation/" + invitationId + "/" + answer, guest).status()).isEqualTo(200);
  }

  private List<Email> reminders(TestUser user) {
    return mail.to(user.email()).stream()
        .filter(letter -> letter.subject().startsWith("Coming up: "))
        .toList();
  }
}
