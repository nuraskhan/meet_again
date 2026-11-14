package kz.nuraskhan.meet_again.model.enums;

/**
 * What people are invited to. Every type carries the values the invitation form is pre-filled with;
 * the creator can change any of them.
 *
 * <p>A new type is added here AND to the check constraint of {@code event.event_type} (a new
 * migration).
 */
public enum EventType {
  BATH("Bathhouse", 0, 15, 5000, 15000),
  FOOD("Eat something together", 0, 5, 3000, 10000),
  COFFEE("Coffee", 0, 3, 1500, 4000),
  BAR("Bar", 0, 10, 5000, 20000),
  WALK("Walk", 0, 5, 0, 0),
  CINEMA("Cinema", 0, 10, 2000, 5000),
  SPORT("Sport", 0, 10, 0, 5000),
  BOARD_GAMES("Board games", 0, 10, 0, 3000),
  OTHER("Hang out", 0, 10, 0, 10000),
  ;

  public final String defaultTitle;
  public final int defaultDistanceMinKm;
  public final int defaultDistanceMaxKm;
  public final int defaultPriceMin;
  public final int defaultPriceMax;

  EventType(String defaultTitle, int defaultDistanceMinKm, int defaultDistanceMaxKm,
            int defaultPriceMin, int defaultPriceMax) {
    this.defaultTitle = defaultTitle;
    this.defaultDistanceMinKm = defaultDistanceMinKm;
    this.defaultDistanceMaxKm = defaultDistanceMaxKm;
    this.defaultPriceMin = defaultPriceMin;
    this.defaultPriceMax = defaultPriceMax;
  }
}
