package kz.nuraskhan.hangelo.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kz.nuraskhan.hangelo.model.entity.Place;
import kz.nuraskhan.hangelo.model.enums.EventType;

/**
 * Invented places for the local database, so that "suggest places" has something to suggest.
 *
 * <p>NONE OF THESE IS A REAL BUSINESS. The names, ratings and prices are made up; only the
 * neighbourhoods of New York City they are put in are real. Every one is saved with
 * {@code sample = true}, and the site shows them with a "Sample" mark.
 *
 * <p>Two per category (three for the most used), spread over Manhattan, Brooklyn and Queens, with
 * prices inside the default budget of the category - so a demo event in any of them finds a choice.
 */
final class SamplePlaces {

  private SamplePlaces() {}

  static List<Place> all() {
    List<Place> places = new ArrayList<>();

    add(places, EventType.BATH, "Steam & Cedar Bathhouse", "Williamsburg, Brooklyn", 40.7081, -73.9571, 4.7, 6000, 12000,
        "Russian banya with a cold plunge and a tea room.");
    add(places, EventType.BATH, "Harbor Hammam", "Financial District, Manhattan", 40.7075, -74.0113, 4.4, 8000, 15000,
        "Turkish steam rooms and scrubs.");
    add(places, EventType.BATH, "Northside Sauna Club", "Astoria, Queens", 40.7644, -73.9235, 4.1, 5000, 9000,
        "Finnish saunas, bring your own towel.");

    add(places, EventType.FOOD, "Lagman House", "East Village, Manhattan", 40.7265, -73.9815, 4.8, 3000, 7000,
        "Hand-pulled noodles and plov.");
    add(places, EventType.FOOD, "Slope Supper", "Park Slope, Brooklyn", 40.6710, -73.9814, 4.5, 4000, 9000,
        "Seasonal plates to share.");
    add(places, EventType.FOOD, "Greenpoint Dumpling Bar", "Greenpoint, Brooklyn", 40.7305, -73.9515, 4.2, 3000, 6000,
        "Pierogi and dumplings, counter seating.");

    add(places, EventType.COFFEE, "Slow Pour", "Lower East Side, Manhattan", 40.7150, -73.9843, 4.9, 1500, 3000,
        "Filter coffee and pastries, quiet in the morning.");
    add(places, EventType.COFFEE, "Bridge Beans", "DUMBO, Brooklyn", 40.7033, -73.9881, 4.6, 2000, 4000,
        "Espresso with a view of the bridge.");
    add(places, EventType.COFFEE, "Uptown Roasters", "Upper West Side, Manhattan", 40.7870, -73.9754, 4.3, 1500, 3500,
        "Roasts its own beans, plenty of tables.");

    add(places, EventType.BAR, "The Copper Still", "Chelsea, Manhattan", 40.7465, -74.0014, 4.6, 6000, 14000,
        "Cocktails and small plates.");
    add(places, EventType.BAR, "Queensboro Taproom", "Long Island City, Queens", 40.7447, -73.9485, 4.3, 5000, 10000,
        "Twenty local beers on tap.");

    add(places, EventType.WALK, "Riverside Loop", "Brooklyn Bridge Park, Brooklyn", 40.7003, -73.9967, 4.8, 0, 0,
        "Two flat miles along the water.");
    add(places, EventType.WALK, "Long Meadow Trail", "Prospect Park, Brooklyn", 40.6602, -73.9690, 4.7, 0, 0,
        "A wide loop through the meadow and the woods.");
    add(places, EventType.WALK, "Reservoir Path", "Central Park, Manhattan", 40.7829, -73.9654, 4.6, 0, 0,
        "A mile and a half around the water.");

    add(places, EventType.CINEMA, "Paper Moon Cinema", "Midtown, Manhattan", 40.7549, -73.9840, 4.5, 2500, 4500,
        "Three screens, new releases and one classic a week.");
    add(places, EventType.CINEMA, "Brownstone Pictures", "Harlem, Manhattan", 40.8116, -73.9465, 4.2, 2000, 4000,
        "Independent films, sofas in the back row.");

    add(places, EventType.SPORT, "Eastside Climbing", "Long Island City, Queens", 40.7447, -73.9485, 4.7, 2500, 5000,
        "Bouldering and rope walls, shoes for rent.");
    add(places, EventType.SPORT, "Pier Courts", "Chelsea, Manhattan", 40.7465, -74.0014, 4.3, 0, 3000,
        "Open basketball and volleyball courts.");

    add(places, EventType.BOARD_GAMES, "Meeple Corner", "Williamsburg, Brooklyn", 40.7081, -73.9571, 4.8, 1000, 3000,
        "Four hundred games and someone to explain the rules.");
    add(places, EventType.BOARD_GAMES, "Dice & Slice", "East Village, Manhattan", 40.7265, -73.9815, 4.4, 1500, 3000,
        "Board games and pizza by the slice.");

    return places;
  }

  private static void add(List<Place> places, EventType category, String name, String neighbourhood,
                          double latitude, double longitude, double rating, int priceMin, int priceMax,
                          String description) {
    Place place = new Place();
    place.name = name;
    place.category = category;
    place.description = description;
    // Not a street address: there is no such business to have one.
    place.address = neighbourhood;
    place.latitude = latitude;
    place.longitude = longitude;
    place.rating = rating;
    place.priceMin = priceMin;
    place.priceMax = priceMax;
    place.sample = true;
    place.createdAt = Instant.now();
    places.add(place);
  }
}
