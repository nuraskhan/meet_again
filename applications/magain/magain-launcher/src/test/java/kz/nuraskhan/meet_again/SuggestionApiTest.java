package kz.nuraskhan.meet_again;

import static kz.nuraskhan.meet_again.jooq.Tables.PLACE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.meet_again.model.entity.Place;
import kz.nuraskhan.meet_again.model.enums.EventType;
import kz.nuraskhan.meet_again.model.repository.PlaceRepository;
import kz.nuraskhan.meet_again.model.rnd.RND;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/**
 * Suggesting places for an event from the locations of the people coming.
 *
 * <p>Places live in one table shared by all tests, and a suggestion looks at everything nearby. So
 * every test works in its own empty spot of the South Pacific, far from the others: people and
 * places are put at so many miles north of that spot.
 */
class SuggestionApiTest extends ApiTestBase {

  /** Miles in one degree of latitude. */
  private static final double MILES_PER_DEGREE = 69.0934;

  @Autowired PlaceRepository placeRepository;

  private double baseLatitude;
  private double baseLongitude;

  @Autowired DSLContext dsl;

  /**
   * WHY the spot is emptied and not just picked at random: the test database is never cleaned, so
   * the places of earlier runs pile up in this part of the ocean. A random spot was empty at first
   * and, a few hundred places later, sometimes had a stranger's place next to it - a test that
   * failed once in a while. Now whatever is left around the spot is switched off before the test
   * puts its own places there. Only this stretch of ocean is touched; nothing real lives in it.
   */
  @BeforeEach
  void pickASpotAndEmptyIt() {
    baseLatitude = -30 - RND.intBetween(0, 20_000) / 1000.0;
    baseLongitude = -90 - RND.intBetween(0, 60_000) / 1000.0;

    // Five degrees each way: far more than any place or person of a test is from its spot.
    dsl.update(PLACE)
        .set(PLACE.DEACTIVATED_AT, Instant.now())
        .where(PLACE.DEACTIVATED_AT.isNull())
        .and(PLACE.LATITUDE.between(baseLatitude - 5, baseLatitude + 5))
        .and(PLACE.LONGITUDE.between(baseLongitude - 5, baseLongitude + 5))
        .execute();
  }

  @Test
  void refresh__suggestsPlacesOfTheEventsCategory_withinEveryonesReach_bestRatedFirst() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = event(creator, "COFFEE", guest);
    locate(creator, eventId, 0, 10);
    locate(guest, eventId, 4, 10);

    place("Good", EventType.COFFEE, 2, 4.0);
    place("Best", EventType.COFFEE, 1, 4.8);
    place("Fine", EventType.COFFEE, 3, 3.5);
    place("Poor", EventType.COFFEE, 2, 2.0);            // fourth by rating: does not make the three
    place("A bar", EventType.BAR, 2, 4.9);              // another category
    Place closed = place("Closed", EventType.COFFEE, 2, 5.0);
    closed.deactivatedAt = Instant.now();               // no longer suggested
    placeRepository.save(closed);
    place("Far away", EventType.COFFEE, 200, 5.0);      // nowhere near

    //
    //
    Response response = post("/event/" + eventId + "/suggestion/refresh", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("suggestedAt").isNull()).isFalse();

    JsonNode suggestions = response.body().get("suggestions");
    assertThat(names(suggestions)).containsExactly("Best", "Good", "Fine");
    assertThat(suggestions.get(0).get("rank").asInt()).isEqualTo(1);
    assertThat(suggestions.get(2).get("rank").asInt()).isEqualTo(3);

    // "Best" is 1 mile north: 1 mile from the creator, 3 from the guest.
    JsonNode best = suggestions.get(0);
    assertThat(best.get("category").asString()).isEqualTo("COFFEE");
    assertThat(best.get("rating").asDouble()).isEqualTo(4.8);
    assertThat(best.get("averageDistanceMi").asDouble()).isCloseTo(2.0, within(0.11));
    assertThat(best.get("farthestDistanceMi").asDouble()).isCloseTo(3.0, within(0.11));
    assertThat(best.get("withinEveryonesReach").asBoolean()).isTrue();
    assertThat(best.get("fitsBudget").asBoolean()).isTrue();
    assertThat(best.get("participantsConsidered").asInt()).isEqualTo(2);
    assertThat(best.get("address").asString()).isNotBlank();

    // Each sees their own distance to the same place.
    assertThat(best.get("myDistanceMi").asDouble()).isCloseTo(1.0, within(0.11));
    JsonNode bestForGuest = get("/event/" + eventId, guest).body().get("suggestions").get(0);
    assertThat(bestForGuest.get("name").asString()).isEqualTo("Best");
    assertThat(bestForGuest.get("myDistanceMi").asDouble()).isCloseTo(3.0, within(0.11));
  }

  @Test
  void refresh__putsAPlaceThatFitsTheBudget_beforeABetterRatedOneThatDoesNot() {
    TestUser creator = register();
    long eventId = event(creator, "COFFEE"); // the budget of a coffee: 1500 to 4000
    locate(creator, eventId, 0, 10);
    place("Affordable", EventType.COFFEE, 1, 3.9, 2000, 3500);
    place("Luxurious", EventType.COFFEE, 1, 4.9, 9000, 20000);

    //
    //
    JsonNode suggestions = post("/event/" + eventId + "/suggestion/refresh", creator).body().get("suggestions");
    //
    //

    assertThat(names(suggestions)).containsExactly("Affordable", "Luxurious");
    assertThat(suggestions.get(0).get("fitsBudget").asBoolean()).isTrue();
    assertThat(suggestions.get(1).get("fitsBudget").asBoolean()).isFalse();
  }

  @Test
  void refresh__stillOffersAPlaceThatIsAStretchForSomeone_andSaysSo() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = event(creator, "COFFEE", guest);
    locate(creator, eventId, 0, 2);   // goes no farther than 2 miles
    locate(guest, eventId, 0, 20);
    place("Five miles out", EventType.COFFEE, 5, 4.5);

    //
    //
    JsonNode suggestions = post("/event/" + eventId + "/suggestion/refresh", creator).body().get("suggestions");
    //
    //

    assertThat(names(suggestions)).containsExactly("Five miles out");
    assertThat(suggestions.get(0).get("withinEveryonesReach").asBoolean()).isFalse();
  }

  @Test
  void refresh__putsAPlaceEveryoneCanReach_beforeABetterRatedOneThatIsAStretch() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = event(creator, "COFFEE", guest);
    locate(creator, eventId, 0, 3);
    locate(guest, eventId, 0, 20);
    place("Near and decent", EventType.COFFEE, 2, 3.0);
    place("Farther and great", EventType.COFFEE, 8, 5.0);

    //
    //
    JsonNode suggestions = post("/event/" + eventId + "/suggestion/refresh", creator).body().get("suggestions");
    //
    //

    assertThat(names(suggestions)).containsExactly("Near and decent", "Farther and great");
  }

  @Test
  void refresh__findsNothing_whenNoPlaceIsNear_andRemembersThatItLooked() {
    TestUser creator = register();
    long eventId = event(creator, "COFFEE");
    locate(creator, eventId, 0, 5);
    place("Too far", EventType.COFFEE, 60, 5.0);

    //
    //
    Response response = post("/event/" + eventId + "/suggestion/refresh", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("suggestions").size()).isZero();
    assertThat(response.body().get("suggestedAt").isNull()).isFalse();
  }

  @Test
  void refresh__forAnEventOfTheTypeOther_considersEveryCategory() {
    TestUser creator = register();
    long eventId = event(creator, "OTHER");
    locate(creator, eventId, 0, 10);
    place("A bar", EventType.BAR, 1, 4.0, 0, 10000);
    place("A cinema", EventType.CINEMA, 1, 4.5, 0, 10000);

    //
    //
    JsonNode suggestions = post("/event/" + eventId + "/suggestion/refresh", creator).body().get("suggestions");
    //
    //

    assertThat(names(suggestions)).containsExactly("A cinema", "A bar");
  }

  @Test
  void suggestions__areKeptAsSaved_untilSomeoneAsksAgain() {
    TestUser creator = register();
    long eventId = event(creator, "COFFEE");
    locate(creator, eventId, 0, 10);
    place("First found", EventType.COFFEE, 1, 4.0);
    post("/event/" + eventId + "/suggestion/refresh", creator);
    place("Opened later, better", EventType.COFFEE, 1, 5.0);

    //
    //
    JsonNode readLater = get("/event/" + eventId, creator).body().get("suggestions");
    JsonNode inTheList = get("/event/list", creator).body().get(0).get("suggestions");
    JsonNode askedAgain = post("/event/" + eventId + "/suggestion/refresh", creator).body().get("suggestions");
    //
    //

    assertThat(names(readLater)).containsExactly("First found");
    assertThat(names(inTheList)).containsExactly("First found");
    assertThat(names(askedAgain)).containsExactly("Opened later, better", "First found");
  }

  @Test
  void suggestions__appearByThemselves_onceTheLocationOfEveryoneComingIsKnown() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = event(creator, "COFFEE", guest);
    accept(creator, eventId, guest);
    place("The one", EventType.COFFEE, 1, 4.0);

    //
    //
    JsonNode afterTheCreator = locate(creator, eventId, 0, 10);
    JsonNode afterTheGuest = locate(guest, eventId, 2, 10);
    //
    //

    // The guest who said yes had not said where they are: nothing yet.
    assertThat(afterTheCreator.get("suggestedAt").isNull()).isTrue();
    assertThat(afterTheCreator.get("suggestions").size()).isZero();

    assertThat(names(afterTheGuest.get("suggestions"))).containsExactly("The one");
    assertThat(afterTheGuest.get("suggestions").get(0).get("participantsConsidered").asInt()).isEqualTo(2);
  }

  @Test
  void suggestions__doNotChangeByThemselves_whenSomeoneMovesLater() {
    TestUser creator = register();
    long eventId = event(creator, "COFFEE");
    place("Near the first spot", EventType.COFFEE, 1, 4.0);
    place("Near the second spot", EventType.COFFEE, 41, 4.0);
    locate(creator, eventId, 0, 5); // alone, so suggested at once

    //
    //
    JsonNode afterMoving = locate(creator, eventId, 40, 5);
    //
    //

    assertThat(names(afterMoving.get("suggestions"))).containsExactly("Near the first spot");
    // Their own distance, though, is from where they are now.
    assertThat(afterMoving.get("suggestions").get(0).get("myDistanceMi").asDouble()).isCloseTo(39.0, within(0.2));
  }

  @Test
  void refresh__needsSomeoneOnTheMap_aTypedPlaceIsNotEnough() {
    TestUser creator = register();
    long eventId = event(creator, "COFFEE");
    post("/event/" + eventId + "/preference", creator, Map.of("locationText", "Downtown", "travelRadiusMi", 5));

    //
    //
    Response response = post("/event/" + eventId + "/suggestion/refresh", creator);
    //
    //

    assertThat(response.status()).isEqualTo(409);
    assertThat(response.body().get("code").asString()).isEqualTo("no_coordinates");
  }

  @Test
  void refresh__isOnlyForPeopleOfTheEvent_whileItIsActive() {
    TestUser creator = register();
    TestUser guest = register();
    TestUser decliner = register();
    TestUser stranger = register();
    long eventId = event(creator, "COFFEE", guest, decliner);
    locate(creator, eventId, 0, 10);
    decline(creator, eventId, decliner);

    //
    //
    Response byGuest = post("/event/" + eventId + "/suggestion/refresh", guest);
    Response byDecliner = post("/event/" + eventId + "/suggestion/refresh", decliner);
    Response byStranger = post("/event/" + eventId + "/suggestion/refresh", stranger);
    Response withoutToken = post("/event/" + eventId + "/suggestion/refresh", null);
    post("/event/" + eventId + "/cancel", creator);
    Response afterCancelling = post("/event/" + eventId + "/suggestion/refresh", creator);
    //
    //

    assertThat(byGuest.status()).isEqualTo(200);
    assertThat(byDecliner.status()).isEqualTo(404);
    assertThat(byStranger.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(afterCancelling.status()).isEqualTo(409);
  }

  @Test
  void suggestions__tellNobodyWhereTheOthersAre() {
    TestUser creator = register();
    TestUser guest = register();
    long eventId = event(creator, "COFFEE", guest);
    locate(creator, eventId, 0, 10);
    JsonNode guestsOwn = locate(guest, eventId, 3.3333, 10);
    String guestLatitude = guestsOwn.get("myPreference").get("latitude").asString();
    place("Somewhere", EventType.COFFEE, 1, 4.0);

    //
    //
    Response forCreator = post("/event/" + eventId + "/suggestion/refresh", creator);
    Response byLink = get("/public/event/" + forCreator.body().get("shareToken").asString(), null);
    //
    //

    assertThat(forCreator.body().get("suggestions").size()).isEqualTo(1);
    assertThat(forCreator.body().toString()).doesNotContain(guestLatitude);
    // The page anyone opens by the link shows no suggestions at all.
    assertThat(byLink.body().has("suggestions")).isFalse();
  }

  // ------------------------------------------------------------------ helpers

  /** An event of {@code creator} of this type, with these guests invited as friends. */
  private long event(TestUser creator, String eventType, TestUser... guests) {
    for (TestUser guest : guests) {
      makeFriends(creator, guest);
    }
    Response response = post("/event/create", creator, Map.of(
        "eventType", eventType,
        "inviteeIds", java.util.Arrays.stream(guests).map(TestUser::id).toList()));
    assertThat(response.status()).isEqualTo(200);
    return response.body().get("id").asLong();
  }

  /** Puts the person on the map so many miles north of the spot of this test. */
  private JsonNode locate(TestUser user, long eventId, double milesNorth, int travelRadiusMi) {
    Response response = post("/event/" + eventId + "/preference", user, Map.of(
        "latitude", northOfBase(milesNorth),
        "longitude", baseLongitude,
        "travelRadiusMi", travelRadiusMi));
    assertThat(response.status()).isEqualTo(200);
    return response.body();
  }

  /** A place so many miles north of the spot, whose prices fit the budget of a coffee. */
  private Place place(String name, EventType category, double milesNorth, double rating) {
    return place(name, category, milesNorth, rating, 2000, 3000);
  }

  private Place place(String name, EventType category, double milesNorth, double rating,
                      int priceMin, int priceMax) {
    Place place = RND.place(category);
    place.name = name;
    place.latitude = northOfBase(milesNorth);
    place.longitude = baseLongitude;
    place.rating = rating;
    place.priceMin = priceMin;
    place.priceMax = priceMax;
    return placeRepository.save(place);
  }

  private double northOfBase(double miles) {
    return Math.round((baseLatitude + miles / MILES_PER_DEGREE) * 100_000) / 100_000.0;
  }

  private void accept(TestUser creator, long eventId, TestUser guest) {
    assertThat(post("/invitation/" + invitationId(creator, eventId, guest) + "/accept", guest).status())
        .isEqualTo(200);
  }

  private void decline(TestUser creator, long eventId, TestUser guest) {
    assertThat(post("/invitation/" + invitationId(creator, eventId, guest) + "/decline", guest).status())
        .isEqualTo(200);
  }

  private long invitationId(TestUser creator, long eventId, TestUser guest) {
    return get("/event/" + eventId, creator).body().get("invitees").valueStream()
        .filter(row -> row.get("userId").asLong() == guest.id())
        .findFirst().orElseThrow()
        .get("invitationId").asLong();
  }

  private static List<String> names(JsonNode suggestions) {
    return suggestions.valueStream().map(suggestion -> suggestion.get("name").asString()).toList();
  }
}
