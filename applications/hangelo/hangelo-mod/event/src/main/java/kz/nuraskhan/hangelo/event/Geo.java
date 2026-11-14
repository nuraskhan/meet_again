package kz.nuraskhan.hangelo.event;

/** Distances on the globe. Coordinates are degrees; distances are miles, as everywhere here. */
final class Geo {

  /** The mean radius of the Earth, miles. */
  private static final double EARTH_RADIUS_MI = 3958.7613;

  /** One degree of latitude, miles. Longitude degrees are this times the cosine of the latitude. */
  static final double MILES_PER_DEGREE = 2 * Math.PI * EARTH_RADIUS_MI / 360;

  private Geo() {}

  /**
   * The distance between two points along the surface (the haversine formula): as the crow flies,
   * not by road. A real trip is longer - by about a third in a city.
   */
  static double distanceMi(double latitude1, double longitude1, double latitude2, double longitude2) {
    double lat1 = Math.toRadians(latitude1);
    double lat2 = Math.toRadians(latitude2);
    double dLat = lat2 - lat1;
    double dLon = Math.toRadians(longitude2 - longitude1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    return 2 * EARTH_RADIUS_MI * Math.asin(Math.min(1, Math.sqrt(a)));
  }

  /** To one decimal: a tenth of a mile is more than anyone needs. */
  static double round1(double miles) {
    return Math.round(miles * 10) / 10.0;
  }
}
