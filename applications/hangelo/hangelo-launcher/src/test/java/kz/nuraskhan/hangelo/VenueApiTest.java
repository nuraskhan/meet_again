package kz.nuraskhan.hangelo;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import kz.nuraskhan.hangelo.model.entity.Place;
import kz.nuraskhan.hangelo.model.enums.EventType;
import kz.nuraskhan.hangelo.model.repository.PlaceRepository;
import kz.nuraskhan.hangelo.model.rnd.RND;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

/** Venues: choosing one for an event, taking it off, and looking through them. */
class VenueApiTest extends ApiTestBase {

  @Autowired PlaceRepository placeRepository;

  @Test
  void attachPlace__putsTheVenueOnTheEvent_forEveryoneWhoSeesIt_andInTheFeed() {
    TestUser creator = register();
    TestUser guest = register();
    makeFriends(creator, guest);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "COFFEE", "visibility", "PUBLIC", "inviteeIds", List.of(guest.id()))).body().get("id").asLong();
    Place place = venue("Coffee Boom " + RND.str(6), EventType.COFFEE, "Testville" + RND.str(6), 4.6, 120);

    //
    //
    Response response = post("/event/" + eventId + "/place/" + place.id + "/attach", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    for (TestUser viewer : List.of(creator, guest)) {
      JsonNode seen = get("/event/" + eventId, viewer).body().get("place");
      assertThat(seen.get("id").asLong()).isEqualTo(place.id);
      assertThat(seen.get("name").asString()).isEqualTo(place.name);
      assertThat(seen.get("address").asString()).isEqualTo(place.address);
      assertThat(seen.get("city").asString()).isEqualTo(place.city);
      assertThat(seen.get("latitude").asDouble()).isEqualTo(place.latitude);
      assertThat(seen.get("longitude").asDouble()).isEqualTo(place.longitude);
      assertThat(seen.get("rating").asDouble()).isEqualTo(4.6);
      assertThat(seen.get("reviewCount").asInt()).isEqualTo(120);
    }
    JsonNode inTheFeed = get("/feed/list", register()).body().valueStream()
        .filter(item -> item.get("id").asLong() == eventId).findFirst().orElseThrow();
    assertThat(inTheFeed.get("placeName").asString()).isEqualTo(place.name);
  }

  @Test
  void detachPlace__takesTheVenueOff() {
    TestUser creator = register();
    long eventId = post("/event/create", creator, Map.of("eventType", "BAR")).body().get("id").asLong();
    Place place = venue("Bar " + RND.str(6), EventType.BAR, "Testville", 4.0, null);
    post("/event/" + eventId + "/place/" + place.id + "/attach", creator);

    //
    //
    Response response = post("/event/" + eventId + "/place/detach", creator);
    //
    //

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.body().get("place").isNull()).isTrue();
    assertThat(get("/event/" + eventId, creator).body().get("place").isNull()).isTrue();
  }

  @Test
  void aNewEvent__hasNoVenue() {
    TestUser creator = register();

    //
    //
    JsonNode event = post("/event/create", creator, Map.of("eventType", "WALK")).body();
    //
    //

    assertThat(event.get("place").isNull()).isTrue();
  }

  @Test
  void attachPlace__isOnlyForTheCreator_ofAnEventThatIsOn_withAVenueThatIsOffered() {
    TestUser creator = register();
    TestUser guest = register();
    makeFriends(creator, guest);
    long eventId = post("/event/create", creator, Map.of(
        "eventType", "FOOD", "inviteeIds", List.of(guest.id()))).body().get("id").asLong();
    Place place = venue("Dinner " + RND.str(6), EventType.FOOD, "Testville", 4.0, null);
    Place closed = venue("Closed " + RND.str(6), EventType.FOOD, "Testville", 4.0, null);
    closed.deactivatedAt = Instant.now();
    placeRepository.save(closed);

    //
    //
    Response byGuest = post("/event/" + eventId + "/place/" + place.id + "/attach", guest);
    Response unknownPlace = post("/event/" + eventId + "/place/" + Long.MAX_VALUE + "/attach", creator);
    Response closedPlace = post("/event/" + eventId + "/place/" + closed.id + "/attach", creator);
    Response withoutToken = post("/event/" + eventId + "/place/" + place.id + "/attach", null);
    post("/event/" + eventId + "/cancel", creator);
    Response afterCancelling = post("/event/" + eventId + "/place/" + place.id + "/attach", creator);
    //
    //

    assertThat(byGuest.status()).isEqualTo(404);
    assertThat(unknownPlace.status()).isEqualTo(404);
    assertThat(unknownPlace.body().get("code").asString()).isEqualTo("place_not_found");
    assertThat(closedPlace.status()).isEqualTo(404);
    assertThat(withoutToken.status()).isEqualTo(401);
    assertThat(afterCancelling.status()).isEqualTo(409);
    assertThat(get("/event/" + eventId, creator).body().get("place").isNull()).isTrue();
  }

  @Test
  void list__filtersByCategoryCityAndText_bestRatedFirst() {
    TestUser reader = register();
    String city = "Testburg" + RND.str(8);
    String mark = RND.str(8);
    venue("Good " + mark, EventType.COFFEE, city, 4.2, 10);
    venue("Best " + mark, EventType.COFFEE, city, 4.9, 3);
    venue("Many reviews " + mark, EventType.COFFEE, city, 4.2, 500);
    venue("A bar " + mark, EventType.BAR, city, 5.0, 1);
    venue("Elsewhere " + mark, EventType.COFFEE, "Otherburg" + RND.str(8), 5.0, 1);
    Place closed = venue("Closed " + mark, EventType.COFFEE, city, 5.0, 1);
    closed.deactivatedAt = Instant.now();
    placeRepository.save(closed);

    //
    //
    JsonNode coffeeInTheCity = get("/place/list?category=COFFEE&city=" + city.toLowerCase(), reader).body();
    JsonNode byText = get("/place/list?city=" + city + "&q=best%20" + mark.toLowerCase(), reader).body();
    JsonNode cities = get("/place/city/list", reader).body();
    //
    //

    // The best rated first; among equals the one more reviews stand behind. No bar, no other
    // city, nothing closed.
    assertThat(coffeeInTheCity.valueStream().map(p -> p.get("name").asString()).toList())
        .containsExactly("Best " + mark, "Many reviews " + mark, "Good " + mark);
    assertThat(byText.valueStream().map(p -> p.get("name").asString()).toList())
        .containsExactly("Best " + mark);
    assertThat(cities.valueStream().map(JsonNode::asString).toList()).contains(city);
  }

  @Test
  void list__fromAPoint_putsTheNearestFirst_andSaysHowFarEachIs() {
    TestUser reader = register();
    String city = "Testburg" + RND.str(8);
    // A point in the open ocean, so that only the venues of this test are near it.
    double lat = -40 - RND.intBetween(0, 9000) / 1000.0;
    double lon = -120 - RND.intBetween(0, 9000) / 1000.0;
    double milesPerDegree = 69.0934;
    venueAt("Three miles", city, lat + 3 / milesPerDegree, lon, 3.0);
    venueAt("One mile", city, lat + 1 / milesPerDegree, lon, 2.0);
    venueAt("Ten miles, the best", city, lat + 10 / milesPerDegree, lon, 5.0);

    //
    //
    JsonNode fromThePoint = post("/place/search", reader,
        Map.of("city", city, "latitude", lat, "longitude", lon)).body();
    JsonNode withoutAPoint = post("/place/search", reader, Map.of("city", city)).body();
    Response halfAPoint = post("/place/search", reader, Map.of("city", city, "latitude", lat));
    Response noSuchPoint = post("/place/search", reader, Map.of("city", city, "latitude", 95, "longitude", 10));
    //
    //

    assertThat(fromThePoint.valueStream().map(p -> p.get("name").asString()).toList())
        .containsExactly("One mile", "Three miles", "Ten miles, the best");
    assertThat(fromThePoint.get(0).get("distanceMi").asDouble()).isEqualTo(1.0);
    assertThat(fromThePoint.get(1).get("distanceMi").asDouble()).isEqualTo(3.0);
    assertThat(fromThePoint.get(2).get("distanceMi").asDouble()).isEqualTo(10.0);

    // Without a point: the best rated first, and no distances.
    assertThat(withoutAPoint.valueStream().map(p -> p.get("name").asString()).toList())
        .containsExactly("Ten miles, the best", "Three miles", "One mile");
    assertThat(withoutAPoint.get(0).get("distanceMi").isNull()).isTrue();

    assertThat(halfAPoint.status()).isEqualTo(400);
    assertThat(noSuchPoint.status()).isEqualTo(400);
  }

  private void venueAt(String name, String city, double latitude, double longitude, double rating) {
    Place place = RND.place(EventType.COFFEE);
    place.name = name;
    place.city = city;
    place.latitude = latitude;
    place.longitude = longitude;
    place.rating = rating;
    placeRepository.save(place);
  }

  @Test
  void list__treatsThePercentSignAsItself() {
    TestUser reader = register();
    String city = "Testburg" + RND.str(8);
    venue("100% coffee", EventType.COFFEE, city, 4.0, null);
    venue("Plain coffee", EventType.COFFEE, city, 4.0, null);

    //
    //
    JsonNode found = get("/place/list?city=" + city + "&q=%25", reader).body();
    //
    //

    assertThat(found.valueStream().map(p -> p.get("name").asString()).toList()).containsExactly("100% coffee");
  }

  private Place venue(String name, EventType category, String city, double rating, Integer reviewCount) {
    Place place = RND.place(category);
    place.name = name;
    place.city = city;
    place.rating = rating;
    place.reviewCount = reviewCount;
    return placeRepository.save(place);
  }
}
