package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** The calendar of availability: busy days, free days, trips - one's own and one's friends'. */
class AvailabilityApiTest extends ApiTestBase {

  @Test
  void add__keepsTheDaysExactlyAsSaid_whateverTheTimeZoneOfTheServer() {
    TestUser me = register();

    //
    //
    Response response = post("/availability/add", me, Map.of(
        "kind", "TRIP", "fromDate", "2031-03-05", "toDate", "2031-03-09",
        "city", "  Almaty ", "note", "Brother's wedding"));
    //
    //

    // A day is a day: "2031-03-05" must come back as "2031-03-05" in Almaty and in Los Angeles.
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("kind").asString()).isEqualTo("TRIP");
    assertThat(response.body().get("fromDate").asString()).isEqualTo("2031-03-05");
    assertThat(response.body().get("toDate").asString()).isEqualTo("2031-03-09");
    assertThat(response.body().get("city").asString()).isEqualTo("Almaty");
    assertThat(response.body().get("note").asString()).isEqualTo("Brother's wedding");

    JsonNode listed = get("/availability/list?from=2031-03-01&to=2031-03-31", me).body();
    assertThat(listed.size()).isEqualTo(1);
    assertThat(listed.get(0).get("fromDate").asString()).isEqualTo("2031-03-05");
    assertThat(listed.get(0).get("toDate").asString()).isEqualTo("2031-03-09");
  }

  @Test
  void list__givesTheMarksThatTouchTheWindow_earliestFirst() {
    TestUser me = register();
    mark(me, "BUSY", "2031-05-28", "2031-06-02");   // reaches into June
    mark(me, "FREE", "2031-06-10", "2031-06-10");   // inside June
    mark(me, "BUSY", "2031-06-30", "2031-07-04");   // starts on the last day of June
    mark(me, "BUSY", "2031-07-01", "2031-07-02");   // July: outside
    mark(me, "BUSY", "2031-05-01", "2031-05-31");   // May: outside

    //
    //
    JsonNode june = get("/availability/list?from=2031-06-01&to=2031-06-30", me).body();
    //
    //

    assertThat(june.valueStream().map(m -> m.get("fromDate").asString()).toList())
        .containsExactly("2031-05-28", "2031-06-10", "2031-06-30");
  }

  @Test
  void friends__seeTheKindTheDaysAndTheCity_butNeverTheNote() {
    TestUser me = register();
    TestUser friend = register();
    TestUser stranger = register();
    makeFriends(me, friend);
    post("/availability/add", friend, Map.of(
        "kind", "TRIP", "fromDate", "2031-08-10", "toDate", "2031-08-12",
        "city", "Astana", "note", "A private reason"));
    mark(stranger, "BUSY", "2031-08-10", "2031-08-12");

    //
    //
    Response response = get("/availability/friends?from=2031-08-01&to=2031-08-31", me);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().size()).isEqualTo(1); // the stranger's calendar is closed
    JsonNode seen = response.body().get(0);
    assertThat(seen.get("userId").asLong()).isEqualTo(friend.id());
    assertThat(seen.get("displayName").asString()).isEqualTo(friend.displayName());
    assertThat(seen.get("kind").asString()).isEqualTo("TRIP");
    assertThat(seen.get("fromDate").asString()).isEqualTo("2031-08-10");
    assertThat(seen.get("toDate").asString()).isEqualTo("2031-08-12");
    assertThat(seen.get("city").asString()).isEqualTo("Astana");
    assertThat(seen.has("note")).isFalse();
    assertThat(response.body().toString()).doesNotContain("A private reason");
  }

  @Test
  void friends__ofOnePerson_givesNothingForSomeoneWhoIsNotAFriend() {
    TestUser me = register();
    TestUser friend = register();
    TestUser stranger = register();
    makeFriends(me, friend);
    mark(friend, "BUSY", "2031-09-01", "2031-09-02");
    mark(stranger, "BUSY", "2031-09-01", "2031-09-02");

    //
    //
    JsonNode ofFriend = get("/availability/friends?from=2031-09-01&to=2031-09-30&userId=" + friend.id(), me).body();
    JsonNode ofStranger = get("/availability/friends?from=2031-09-01&to=2031-09-30&userId=" + stranger.id(), me).body();
    //
    //

    assertThat(ofFriend.size()).isEqualTo(1);
    assertThat(ofStranger.size()).isZero();
  }

  @Test
  void friends__stopSeeingTheCalendar_whenTheFriendshipEnds() {
    TestUser me = register();
    TestUser friend = register();
    makeFriends(me, friend);
    mark(friend, "BUSY", "2031-10-01", "2031-10-02");
    post("/friend/" + friend.id() + "/remove", me);

    //
    //
    JsonNode seen = get("/availability/friends?from=2031-10-01&to=2031-10-31", me).body();
    //
    //

    assertThat(seen.size()).isZero();
  }

  @Test
  void remove__takesTheMarkOut_andOnlyItsOwnerCan() {
    TestUser me = register();
    TestUser other = register();
    long id = mark(me, "BUSY", "2031-11-01", "2031-11-03");

    //
    //
    Response byOther = post("/availability/" + id + "/remove", other);
    Response byMe = post("/availability/" + id + "/remove", me);
    Response again = post("/availability/" + id + "/remove", me);
    //
    //

    assertThat(byOther.status()).isEqualTo(404);
    assertThat(byMe.status()).isEqualTo(200);
    assertThat(again.status()).isEqualTo(404);
    assertThat(get("/availability/list?from=2031-11-01&to=2031-11-30", me).body().size()).isZero();
  }

  @Test
  void add__refusesWhatMakesNoSense() {
    TestUser me = register();

    //
    //
    Response backwards = post("/availability/add", me,
        Map.of("kind", "BUSY", "fromDate", "2031-12-10", "toDate", "2031-12-01"));
    Response tripToNowhere = post("/availability/add", me,
        Map.of("kind", "TRIP", "fromDate", "2031-12-01", "toDate", "2031-12-02", "city", "  "));
    Response tooLong = post("/availability/add", me,
        Map.of("kind", "BUSY", "fromDate", "2031-01-01", "toDate", "2032-06-01"));
    Response unknownKind = post("/availability/add", me,
        Map.of("kind", "ASLEEP", "fromDate", "2031-12-01", "toDate", "2031-12-02"));
    Response withoutToken = post("/availability/add", null,
        Map.of("kind", "BUSY", "fromDate", "2031-12-01", "toDate", "2031-12-02"));
    Response hugeWindow = get("/availability/list?from=2031-01-01&to=2033-01-01", me);
    //
    //

    assertThat(backwards.status()).isEqualTo(400);
    assertThat(backwards.body().get("code").asString()).isEqualTo("bad_dates");
    assertThat(tripToNowhere.status()).isEqualTo(400);
    assertThat(tripToNowhere.body().get("code").asString()).isEqualTo("no_city");
    assertThat(tooLong.status()).isEqualTo(400);
    assertThat(unknownKind.status()).isEqualTo(400);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(hugeWindow.status()).isEqualTo(400);
    assertThat(get("/availability/list?from=2031-01-01&to=2031-12-31", me).body().size()).isZero();
  }

  @Test
  void add__forBusyDays_keepsNoCity() {
    TestUser me = register();

    //
    //
    Response response = post("/availability/add", me, Map.of(
        "kind", "BUSY", "fromDate", "2031-04-01", "toDate", "2031-04-01", "city", "Almaty"));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("city").isNull()).isTrue();
  }

  /** @return the id of the mark */
  private long mark(TestUser user, String kind, String from, String to) {
    Response response = post("/availability/add", user,
        kind.equals("TRIP")
            ? Map.of("kind", kind, "fromDate", from, "toDate", to, "city", "Almaty")
            : Map.of("kind", kind, "fromDate", from, "toDate", to));
    assertThat(response.status()).isEqualTo(200);
    return response.body().get("id").asLong();
  }
}
