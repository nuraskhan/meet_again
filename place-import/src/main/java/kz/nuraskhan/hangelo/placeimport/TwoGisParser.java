package kz.nuraskhan.hangelo.placeimport;

import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads an answer of the 2GIS Places API ({@code GET /3.0/items}) into venues.
 *
 * <p>The shape, as documented (docs.2gis.com, Places API, reference 3.0/items):
 * <pre>
 * { "meta": { "code": 200, ... },
 *   "result": { "total": 123, "items": [ {
 *       "id": "...", "name": "...", "address_name": "...", "full_address_name": "...",
 *       "point": { "lat": 51.1, "lon": 71.4 },
 *       "reviews": { "general_rating": 4.6, "general_review_count": 321, ... } } ] } }
 * </pre>
 * {@code point} and {@code reviews} are present only when asked for in {@code fields}, and
 * {@code reviews} only if the key is allowed to see it - so both are optional here.
 */
final class TwoGisParser {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private TwoGisParser() {
  }

  /**
   * @throws IllegalStateException if the answer is not a success ({@code meta.code} other than 200,
   *                               except 404 "nothing found", which is an empty list)
   */
  static List<Venue> parse(String body) {
    JsonNode root = JSON.readTree(body);

    int code = root.path("meta").path("code").asInt(0);
    if (code == 404) {
      return List.of(); // the directory's way of saying "nothing matches"
    }
    if (code != 200) {
      String message = root.path("meta").path("error").path("message").asString("");
      throw new IllegalStateException("2GIS answered " + code + (message.isEmpty() ? "" : ": " + message));
    }

    List<Venue> venues = new ArrayList<>();
    for (JsonNode item : root.path("result").path("items")) {
      String id = item.path("id").asString("");
      String name = item.path("name").asString("").trim();
      JsonNode point = item.path("point");

      // Without an id it cannot be recognised next time; without coordinates it cannot be
      // suggested by distance; without a name it cannot be shown.
      if (id.isEmpty() || name.isEmpty() || !point.path("lat").isNumber() || !point.path("lon").isNumber()) {
        continue;
      }

      String address = item.path("address_name").asString("").trim();
      if (address.isEmpty()) {
        address = item.path("full_address_name").asString("").trim();
      }

      JsonNode reviews = item.path("reviews");
      Double rating = number(reviews.path("general_rating"));
      Integer reviewCount = reviews.path("general_review_count").isNumber()
          ? reviews.path("general_review_count").asInt()
          : null;
      // The directory writes 0 for "no rating yet"; for us that is "unknown", not "the worst".
      if (rating != null && (rating <= 0 || rating > 5)) {
        rating = null;
      }

      venues.add(new Venue(id, name, address, point.path("lat").asDouble(), point.path("lon").asDouble(),
          rating, reviewCount));
    }
    return venues;
  }

  private static Double number(JsonNode node) {
    if (node.isNumber()) {
      return node.asDouble();
    }
    // Some answers carry the rating as text: "4.6".
    if (node.isString()) {
      try {
        return Double.valueOf(node.asString().trim());
      } catch (NumberFormatException e) {
        return null;
      }
    }
    return null;
  }
}
