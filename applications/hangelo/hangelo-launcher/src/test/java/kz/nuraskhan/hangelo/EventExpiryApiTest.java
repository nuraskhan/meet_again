package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.event.expiry.EventExpiryJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * An event whose dates are behind becomes EXPIRED. The timer is off in tests: each test runs the
 * job itself, for the moment it chooses.
 *
 * <p>A run looks at the whole database and may expire events of tests that have already finished.
 * That is harmless: every test looks only at the events it created.
 */
class EventExpiryApiTest extends ApiTestBase {

  @Autowired EventExpiryJob job;

  @Test
  void expireDue__marksAnEventWhoseOnlyDateIsWellBehind() {
    TestUser creator = register();
    Instant now = Instant.now();
    long eventId = eventWithDates(creator, now.plus(1, ChronoUnit.HOURS)).get("id").asLong();

    //
    //
    job.expireDue(now.plus(5, ChronoUnit.HOURS)); // four hours after the date
    //
    //

    assertThat(status(creator, eventId)).isEqualTo("EXPIRED");
  }

  @Test
  void expireDue__leavesAnEventAlone_forThreeHoursAfterItsDate() {
    TestUser creator = register();
    Instant now = Instant.now();
    long eventId = eventWithDates(creator, now.plus(1, ChronoUnit.HOURS)).get("id").asLong();

    //
    //
    job.expireDue(now.plus(3, ChronoUnit.HOURS)); // two hours after the date: still going on
    String twoHoursAfter = status(creator, eventId);
    job.expireDue(now.plus(4, ChronoUnit.HOURS).plus(1, ChronoUnit.MINUTES)); // three hours and a minute
    String justOverThreeHoursAfter = status(creator, eventId);
    //
    //

    assertThat(twoHoursAfter).isEqualTo("ACTIVE");
    assertThat(justOverThreeHoursAfter).isEqualTo("EXPIRED");
  }

  @Test
  void expireDue__waitsForTheLastOfSeveralDates() {
    TestUser creator = register();
    Instant now = Instant.now();
    long eventId = eventWithDates(creator, now.plus(1, ChronoUnit.HOURS), now.plus(50, ChronoUnit.HOURS))
        .get("id").asLong();

    //
    //
    job.expireDue(now.plus(10, ChronoUnit.HOURS)); // the first date is long behind, the second ahead
    String betweenTheDates = status(creator, eventId);
    job.expireDue(now.plus(54, ChronoUnit.HOURS));
    String afterTheLastDate = status(creator, eventId);
    //
    //

    assertThat(betweenTheDates).isEqualTo("ACTIVE");
    assertThat(afterTheLastDate).isEqualTo("EXPIRED");
  }

  @Test
  void expireDue__neverTouchesAnEventWithoutDates_orACancelledOne() {
    TestUser creator = register();
    Instant now = Instant.now();
    long undated = eventWithDates(creator).get("id").asLong();
    long cancelled = eventWithDates(creator, now.plus(1, ChronoUnit.HOURS)).get("id").asLong();
    post("/event/" + cancelled + "/cancel", creator);

    //
    //
    job.expireDue(now.plus(1000, ChronoUnit.DAYS));
    //
    //

    assertThat(status(creator, undated)).isEqualTo("ACTIVE");
    assertThat(status(creator, cancelled)).isEqualTo("CANCELLED");
  }

  @Test
  void expireDue__tellsHowManyItMarked_andMarksEachOnce() {
    TestUser creator = register();
    Instant now = Instant.now();
    Instant later = now.plus(5, ChronoUnit.HOURS);
    job.expireDue(later); // whatever other tests left behind is expired first
    eventWithDates(creator, now.plus(1, ChronoUnit.HOURS));
    eventWithDates(creator, now.plus(1, ChronoUnit.HOURS));

    //
    //
    int first = job.expireDue(later);
    int second = job.expireDue(later);
    //
    //

    assertThat(first).isEqualTo(2);
    assertThat(second).isZero();
  }

  @Test
  void anExpiredEvent__isClosedForJoiningAnsweringAndCancelling_butStillReadable() {
    TestUser creator = register();
    TestUser invited = register();
    TestUser stranger = register();
    makeFriends(creator, invited);
    Instant now = Instant.now();
    JsonNode event = post("/event/create", creator, Map.of(
        "eventType", "FOOD",
        "proposedTimes", List.of(now.plus(1, ChronoUnit.HOURS).toString()),
        "inviteeIds", List.of(invited.id()))).body();
    long eventId = event.get("id").asLong();
    long invitationId = event.get("invitees").get(0).get("invitationId").asLong();
    String shareToken = event.get("shareToken").asString();
    job.expireDue(now.plus(5, ChronoUnit.HOURS));

    //
    //
    Response join = post("/event/join/" + shareToken, stranger);
    Response accept = post("/invitation/" + invitationId + "/accept", invited);
    Response decline = post("/invitation/" + invitationId + "/decline", invited);
    Response cancel = post("/event/" + eventId + "/cancel", creator);
    //
    //

    for (Response refused : List.of(join, accept, decline, cancel)) {
      assertThat(refused.status()).isEqualTo(409);
      assertThat(refused.body().get("code").asString()).isEqualTo("event_expired");
    }

    // Still there to look at: for its people, by its link, and in the chat.
    assertThat(status(creator, eventId)).isEqualTo("EXPIRED");
    assertThat(status(invited, eventId)).isEqualTo("EXPIRED");
    assertThat(get("/public/event/" + shareToken, null).body().get("status").asString()).isEqualTo("EXPIRED");
    assertThat(get("/chat/" + event.get("chatId").asLong() + "/message/list", invited).status()).isEqualTo(200);
    // An unanswered invitation to something that is over is not waiting for an answer any more.
    assertThat(get("/invitation/incoming", invited).body().size()).isZero();
  }

  // ------------------------------------------------------------------ helpers

  /** An event of {@code creator} with these proposed dates; no guests. */
  private JsonNode eventWithDates(TestUser creator, Instant... proposedTimes) {
    Response response = post("/event/create", creator, Map.of(
        "eventType", "WALK",
        "proposedTimes", Arrays.stream(proposedTimes).map(Instant::toString).toList()));
    assertThat(response.status()).isEqualTo(200);
    return response.body();
  }

  private String status(TestUser as, long eventId) {
    return get("/event/" + eventId, as).body().get("status").asString();
  }
}
