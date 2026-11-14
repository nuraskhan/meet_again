package kz.nuraskhan.meet_again;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/** What a guest says about themselves for an event: their place and how far they travel. */
class PreferenceApiTest extends ApiTestBase {

  @Test
  void savePreference__withDeviceLocation_givesCoordinatesBackToTheOwnerOnly() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser otherGuest = register();
    long eventId = eventWithGuests(creator, guest, otherGuest);

    //
    //
    Response response = post("/event/" + eventId + "/preference", guest,
        Map.of("latitude", 40.7128, "longitude", -74.006, "travelRadiusMi", 12));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode mine = response.body().get("myPreference");
    assertThat(mine.get("latitude").asDouble()).isEqualTo(40.7128);
    assertThat(mine.get("longitude").asDouble()).isEqualTo(-74.006);
    assertThat(mine.get("locationText").isNull()).isTrue();
    assertThat(mine.get("travelRadiusMi").asInt()).isEqualTo(12);

    // The others learn the radius and that a location was shared - never where it is.
    for (TestUser viewer : List.of(creator, otherGuest)) {
      Response seen = get("/event/" + eventId, viewer);
      JsonNode row = invitee(seen.body(), guest);
      assertThat(row.get("travelRadiusMi").asInt()).isEqualTo(12);
      assertThat(row.get("locationShared").asBoolean()).isTrue();
      assertThat(row.get("locationText").isNull()).isTrue();
      assertThat(seen.body().get("myPreference").isNull()).isTrue();
      assertThat(seen.body().toString()).doesNotContain("40.7128").doesNotContain("74.006");
    }
  }

  @Test
  void savePreference__withTypedPlace_showsThePlaceToTheOthers() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = eventWithGuests(creator, guest);

    //
    //
    Response response = post("/event/" + eventId + "/preference", guest,
        Map.of("locationText", "  Brooklyn, near Prospect Park ", "travelRadiusMi", 5));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("myPreference").get("locationText").asString())
        .isEqualTo("Brooklyn, near Prospect Park");

    JsonNode row = invitee(get("/event/" + eventId, creator).body(), guest);
    assertThat(row.get("locationText").asString()).isEqualTo("Brooklyn, near Prospect Park");
    assertThat(row.get("travelRadiusMi").asInt()).isEqualTo(5);
    assertThat(row.get("locationShared").asBoolean()).isFalse();
  }

  @Test
  void savePreference__again_replacesWhatWasSaid() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = eventWithGuests(creator, guest);
    post("/event/" + eventId + "/preference", guest,
        Map.of("latitude", 34.05, "longitude", -118.24, "travelRadiusMi", 30));

    //
    //
    Response response = post("/event/" + eventId + "/preference", guest,
        Map.of("locationText", "Santa Monica", "travelRadiusMi", 8));
    //
    //

    assertThat(response.status()).isEqualTo(200);
    JsonNode mine = response.body().get("myPreference");
    assertThat(mine.get("latitude").isNull()).isTrue();
    assertThat(mine.get("longitude").isNull()).isTrue();
    assertThat(mine.get("locationText").asString()).isEqualTo("Santa Monica");
    assertThat(mine.get("travelRadiusMi").asInt()).isEqualTo(8);
    assertThat(invitee(response.body(), guest).get("locationShared").asBoolean()).isFalse();
  }

  @Test
  void savePreference__isKeptPerEvent() {
    TestUser creator = register();
    TestUser guest = register();
    long event1 = eventWithGuests(creator, guest);
    long event2 = eventWithGuests(creator, guest);

    //
    //
    post("/event/" + event1 + "/preference", guest, Map.of("locationText", "Home", "travelRadiusMi", 3));
    //
    //

    assertThat(get("/event/" + event1, guest).body().get("myPreference").get("travelRadiusMi").asInt())
        .isEqualTo(3);
    assertThat(get("/event/" + event2, guest).body().get("myPreference").isNull()).isTrue();
    assertThat(invitee(get("/event/" + event2, creator).body(), guest).get("travelRadiusMi").isNull())
        .isTrue();
  }

  @Test
  void savePreference__refusesIncompleteOrImpossibleValues() {
    TestUser creator = register();
    TestUser guest = register();
    String path = "/event/" + eventWithGuests(creator, guest) + "/preference";

    Map<String, Object> noRadius = new HashMap<>();
    noRadius.put("locationText", "Somewhere");

    //
    //
    Response noPlace = post(path, guest, Map.of("travelRadiusMi", 5));
    Response latitudeAlone = post(path, guest, Map.of("latitude", 40.0, "travelRadiusMi", 5));
    Response latitudeOffTheGlobe =
        post(path, guest, Map.of("latitude", 91.0, "longitude", 0.0, "travelRadiusMi", 5));
    Response zeroRadius = post(path, guest, Map.of("locationText", "Somewhere", "travelRadiusMi", 0));
    Response missingRadius = post(path, guest, noRadius);
    //
    //

    assertThat(noPlace.status()).isEqualTo(400);
    assertThat(noPlace.body().get("code").asString()).isEqualTo("no_location");
    assertThat(latitudeAlone.status()).isEqualTo(400);
    assertThat(latitudeAlone.body().get("code").asString()).isEqualTo("bad_location");
    assertThat(latitudeOffTheGlobe.status()).isEqualTo(400);
    assertThat(zeroRadius.status()).isEqualTo(400);
    assertThat(missingRadius.status()).isEqualTo(400);
  }

  @Test
  void savePreference__isOnlyForAGuestWhoHasNotDeclined() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser decliner = register();
    TestUser stranger = register();
    long eventId = eventWithGuests(creator, guest, decliner);
    long declinersInvitation =
        invitee(get("/event/" + eventId, creator).body(), decliner).get("invitationId").asLong();
    post("/invitation/" + declinersInvitation + "/decline", decliner);
    Map<String, Object> body = Map.of("locationText", "Somewhere", "travelRadiusMi", 5);

    //
    //
    Response byCreator = post("/event/" + eventId + "/preference", creator, body);
    Response byStranger = post("/event/" + eventId + "/preference", stranger, body);
    Response byDecliner = post("/event/" + eventId + "/preference", decliner, body);
    //
    //

    assertThat(byCreator.status()).isEqualTo(404);
    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(byDecliner.status()).isEqualTo(409);
    assertThat(byDecliner.body().get("code").asString()).isEqualTo("invitation_declined");
  }

  @Test
  void publicEvent__showsNoPreferences() {
    TestUser creator = register();
    TestUser guest = register();
    JsonNode created = post("/event/create", creator, Map.of("eventType", "WALK")).body();
    post("/event/join/" + created.get("shareToken").asString(), guest);
    post("/event/" + created.get("id").asLong() + "/preference", guest,
        Map.of("latitude", 47.6062, "longitude", -122.3321, "locationText", "Capitol Hill",
            "travelRadiusMi", 7));

    //
    //
    Response response = get("/public/event/" + created.get("shareToken").asString(), null);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().toString())
        .doesNotContain("47.6062").doesNotContain("122.3321").doesNotContain("Capitol Hill");
  }

  /** An event of {@code creator} whose guests are the given people (invited as friends). */
  private long eventWithGuests(TestUser creator, TestUser... guests) {
    for (TestUser guest : guests) {
      if (get("/friend/list", creator).body().valueStream()
          .noneMatch(friend -> friend.get("userId").asLong() == guest.id())) {
        makeFriends(creator, guest);
      }
    }
    Response response = post("/event/create", creator, Map.of(
        "eventType", "FOOD",
        "inviteeIds", java.util.Arrays.stream(guests).map(TestUser::id).toList()));
    assertThat(response.status()).isEqualTo(200);
    return response.body().get("id").asLong();
  }

  private static JsonNode invitee(JsonNode event, TestUser user) {
    return event.get("invitees").valueStream()
        .filter(row -> row.get("userId").asLong() == user.id())
        .findFirst().orElseThrow();
  }
}
