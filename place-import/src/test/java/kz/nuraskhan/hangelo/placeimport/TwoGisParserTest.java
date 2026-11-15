package kz.nuraskhan.hangelo.placeimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class TwoGisParserTest {

  @Test
  void parse__readsNameAddressCoordinatesAndRating() {
    String body = """
        { "meta": { "api_version": "3.0", "code": 200 },
          "result": { "total": 2, "items": [
            { "id": "70000001000000001", "name": " Coffee Boom ", "address_name": "Kabanbay Batyr, 21",
              "type": "branch", "point": { "lat": 51.1283, "lon": 71.4305 },
              "reviews": { "general_rating": 4.6, "general_review_count": 321, "rating": "4.6" } },
            { "id": "70000001000000002", "name": "Arasan", "full_address_name": "Almaty, Tulebaev, 78",
              "point": { "lat": 43.2609, "lon": 76.9462 } }
          ] } }
        """;

    //
    //
    List<Venue> venues = TwoGisParser.parse(body);
    //
    //

    assertThat(venues).containsExactly(
        new Venue("70000001000000001", "Coffee Boom", "Kabanbay Batyr, 21", 51.1283, 71.4305, 4.6, 321),
        // No reviews in the answer (the key may not see them): the rating is unknown, not zero.
        new Venue("70000001000000002", "Arasan", "Almaty, Tulebaev, 78", 43.2609, 76.9462, null, null));
  }

  @Test
  void parse__leavesOutWhatCannotBeUsed_andTreatsAZeroRatingAsUnknown() {
    String body = """
        { "meta": { "code": 200 },
          "result": { "items": [
            { "id": "1", "name": "No coordinates", "address_name": "Somewhere" },
            { "name": "No id", "point": { "lat": 1, "lon": 2 } },
            { "id": "3", "name": "  ", "point": { "lat": 1, "lon": 2 } },
            { "id": "4", "name": "Not rated yet", "address_name": "Abay, 1", "point": { "lat": 1.5, "lon": 2.5 },
              "reviews": { "general_rating": 0, "general_review_count": 0 } },
            { "id": "5", "name": "Rating as text", "address_name": "Abay, 2", "point": { "lat": 1.5, "lon": 2.5 },
              "reviews": { "general_rating": "4.2", "general_review_count": 7 } }
          ] } }
        """;

    //
    //
    List<Venue> venues = TwoGisParser.parse(body);
    //
    //

    assertThat(venues).containsExactly(
        new Venue("4", "Not rated yet", "Abay, 1", 1.5, 2.5, null, 0),
        new Venue("5", "Rating as text", "Abay, 2", 1.5, 2.5, 4.2, 7));
  }

  @Test
  void parse__nothingFound_isAnEmptyList_andARefusalIsAnError() {
    String nothing = """
        { "meta": { "code": 404, "error": { "message": "Results not found", "type": "itemNotFound" } } }
        """;
    String refused = """
        { "meta": { "code": 403, "error": { "message": "Authorization error: key is blocked" } } }
        """;

    //
    //
    List<Venue> venues = TwoGisParser.parse(nothing);
    //
    //

    assertThat(venues).isEmpty();
    assertThatThrownBy(() -> TwoGisParser.parse(refused))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("403")
        .hasMessageContaining("key is blocked");
  }

  @Test
  void mostPopular__takesThoseWithTheMostReviews_andKeepsTheDirectorysOrderWhenThereAreNone() {
    Venue few = new Venue("1", "Few", "", 0, 0, 5.0, 3);
    Venue many = new Venue("2", "Many", "", 0, 0, 4.1, 900);
    Venue some = new Venue("3", "Some", "", 0, 0, 4.8, 40);
    Venue firstUnknown = new Venue("4", "First", "", 0, 0, null, null);
    Venue secondUnknown = new Venue("5", "Second", "", 0, 0, null, null);

    //
    //
    List<Venue> byReviews = PlaceImport.mostPopular(List.of(few, many, some), 2);
    List<Venue> withoutReviews = PlaceImport.mostPopular(List.of(firstUnknown, secondUnknown), 2);
    //
    //

    assertThat(byReviews).containsExactly(many, some);
    assertThat(withoutReviews).containsExactly(firstUnknown, secondUnknown);
  }
}
