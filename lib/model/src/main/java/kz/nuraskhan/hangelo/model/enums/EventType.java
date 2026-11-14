package kz.nuraskhan.hangelo.model.enums;

/**
 * What people are invited to. Every type carries the values the invitation form is pre-filled with;
 * the creator can change any of them. Distances are in miles.
 *
 * <p>A new type is added here AND to the check constraint of {@code event.event_type} (a new
 * migration).
 */
public enum EventType {
  BATH("Bathhouse", 0, 10, 5000, 15000),
  FOOD("Eat something together", 0, 3, 3000, 10000),
  COFFEE("Coffee", 0, 2, 1500, 4000),
  BAR("Bar", 0, 6, 5000, 20000),
  WALK("Walk", 0, 3, 0, 0),
  CINEMA("Cinema", 0, 6, 2000, 5000),
  SPORT("Sport", 0, 6, 0, 5000),
  BOARD_GAMES("Board games", 0, 6, 0, 3000),
  OTHER("Hang out", 0, 6, 0, 10000),
  ;

  public final String defaultTitle;
  public final int defaultDistanceMinMi;
  public final int defaultDistanceMaxMi;
  public final int defaultPriceMin;
  public final int defaultPriceMax;

  EventType(String defaultTitle, int defaultDistanceMinMi, int defaultDistanceMaxMi,
            int defaultPriceMin, int defaultPriceMax) {
    this.defaultTitle = defaultTitle;
    this.defaultDistanceMinMi = defaultDistanceMinMi;
    this.defaultDistanceMaxMi = defaultDistanceMaxMi;
    this.defaultPriceMin = defaultPriceMin;
    this.defaultPriceMax = defaultPriceMax;
  }
}
