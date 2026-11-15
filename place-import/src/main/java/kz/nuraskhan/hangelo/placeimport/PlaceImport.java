package kz.nuraskhan.hangelo.placeimport;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the popular venues of every category for Astana and Almaty from the 2GIS Places API into
 * the table {@code place}.
 *
 * <p>"Popular" is what the directory itself can tell: the venues the most reviews stand behind;
 * when the key is not allowed to see reviews - the order the directory returns them in (its own
 * relevance).
 *
 * <p>Safe to run again: a venue is recognised by its id in the directory and updated, never
 * doubled. Venues that came from elsewhere (entered by hand, demo samples) are not touched.
 *
 * <p>What is NOT taken: photographs (they belong to their authors), phone numbers and sites (the
 * API gives them only to keys with an extra permission), prices (the directory has none - they
 * stay unknown, see migration V016).
 *
 * <p>Arguments: the JDBC address, the user, the password of the database - or none, and then they
 * are read from HANGELO_IMPORT_DB_URL, HANGELO_IMPORT_DB_USER, HANGELO_IMPORT_DB_PASSWORD.
 * The key of the API is always read from HANGELO_2GIS_KEY.
 */
public final class PlaceImport {

  /** How many venues of one category are kept for one city. */
  static final int PER_CATEGORY = 20;

  /** How many the directory is asked for, to choose those from: its largest page. */
  private static final int PAGE_SIZE = 50;

  /** A city: its name as people here write it, its centre, and how far around it to look. */
  record City(String name, double latitude, double longitude, int radiusM) {
  }

  static final List<City> CITIES = List.of(
      new City("Astana", 51.1694, 71.4491, 15_000),
      new City("Almaty", 43.2389, 76.8897, 15_000));

  /**
   * What to ask the directory for, for each category of ours. In Russian: that is the language
   * the directory of these cities is kept in, and the words people there search by.
   */
  static final Map<String, String> QUERIES = categories();

  private static Map<String, String> categories() {
    Map<String, String> queries = new LinkedHashMap<>();
    queries.put("BATH", "баня сауна");
    queries.put("FOOD", "ресторан");
    queries.put("COFFEE", "кофейня");
    queries.put("BAR", "бар");
    queries.put("WALK", "парк");
    queries.put("CINEMA", "кинотеатр");
    queries.put("SPORT", "спортивный комплекс");
    queries.put("BOARD_GAMES", "антикафе настольные игры");
    queries.put("OTHER", "развлекательный центр");
    return queries;
  }

  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(15))
      .build();

  private PlaceImport() {
  }

  public static void main(String[] args) throws Exception {
    String key = System.getenv("HANGELO_2GIS_KEY");
    if (key == null || key.isBlank()) {
      System.err.println("HANGELO_2GIS_KEY is not set: get a key at dev.2gis.com and put it into that variable");
      System.exit(2);
    }

    String url = args.length > 0 ? args[0] : System.getenv("HANGELO_IMPORT_DB_URL");
    String user = args.length > 1 ? args[1] : System.getenv("HANGELO_IMPORT_DB_USER");
    String password = args.length > 2 ? args[2] : System.getenv("HANGELO_IMPORT_DB_PASSWORD");
    if (url == null || url.isBlank()) {
      System.err.println("No database: pass <jdbc url> <user> <password>, or set HANGELO_IMPORT_DB_URL/USER/PASSWORD");
      System.exit(2);
    }

    int loaded = 0;
    try (Connection connection = DriverManager.getConnection(url, user, password)) {
      for (City city : CITIES) {
        for (Map.Entry<String, String> category : QUERIES.entrySet()) {
          List<Venue> found = TwoGisParser.parse(ask(key, city, category.getValue()));
          List<Venue> chosen = mostPopular(found, PER_CATEGORY);
          for (Venue venue : chosen) {
            save(connection, venue, category.getKey(), city.name());
          }
          loaded += chosen.size();
          System.out.println(city.name() + " / " + category.getKey() + ": " + chosen.size()
              + " of " + found.size() + " found");
          // The directory limits how often a key may ask. There is no hurry.
          Thread.sleep(300);
        }
      }
    }
    System.out.println("Done: " + loaded + " venues loaded or updated");
  }

  /**
   * The venues the most reviews stand behind. Without review counts (the key may not see them)
   * the order of the directory is kept - the sort is stable, and all compare as equal.
   */
  static List<Venue> mostPopular(List<Venue> venues, int howMany) {
    return venues.stream()
        .sorted(Comparator.comparingInt((Venue venue) -> venue.reviewCount() == null ? 0 : venue.reviewCount())
            .reversed())
        .limit(howMany)
        .toList();
  }

  private static String ask(String key, City city, String query) throws IOException, InterruptedException {
    // type=branch: businesses and their branches - not streets, buildings or bus stops that
    // happen to carry the word.
    String address = "https://catalog.api.2gis.com/3.0/items"
        + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
        + "&point=" + city.longitude() + "," + city.latitude() // the API wants longitude first
        + "&radius=" + city.radiusM()
        + "&type=branch"
        + "&page_size=" + PAGE_SIZE
        + "&locale=ru_KZ"
        + "&fields=items.point,items.reviews,items.full_address_name"
        + "&key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

    HttpResponse<String> response = HTTP.send(
        HttpRequest.newBuilder(URI.create(address)).timeout(Duration.ofSeconds(30)).GET().build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    // WHY the address is not in the message: it carries the key.
    if (response.statusCode() / 100 != 2 && response.statusCode() != 404) {
      throw new IOException("2GIS answered HTTP " + response.statusCode() + " for \"" + query + "\" in " + city.name());
    }
    return response.body();
  }

  /**
   * Adds the venue, or updates it if it was loaded before. The category and the prices of a venue
   * already there are left alone: the first category it was found under stays (a place can match
   * two searches), and prices are not something the directory knows.
   */
  private static void save(Connection connection, Venue venue, String category, String city) throws SQLException {
    String sql = """
        insert into place (name, category, address, latitude, longitude, rating, review_count,
                           city, source, external_id, sample, created_at)
        values (?, ?, ?, ?, ?, ?, ?, ?, '2GIS', ?, false, ?)
        on conflict (source, external_id) where external_id is not null do update
        set name = excluded.name,
            address = excluded.address,
            latitude = excluded.latitude,
            longitude = excluded.longitude,
            rating = excluded.rating,
            review_count = excluded.review_count,
            city = excluded.city
        """;
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, venue.name());
      statement.setString(2, category);
      statement.setString(3, venue.address().isEmpty() ? city : venue.address());
      statement.setDouble(4, venue.latitude());
      statement.setDouble(5, venue.longitude());
      if (venue.rating() == null) {
        statement.setNull(6, Types.DOUBLE);
      } else {
        statement.setDouble(6, venue.rating());
      }
      if (venue.reviewCount() == null) {
        statement.setNull(7, Types.INTEGER);
      } else {
        statement.setInt(7, venue.reviewCount());
      }
      statement.setString(8, city);
      statement.setString(9, venue.externalId());
      statement.setTimestamp(10, Timestamp.from(Instant.now()));
      statement.executeUpdate();
    }
  }
}
