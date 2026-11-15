package kz.nuraskhan.hangelo.model.enums;

/**
 * What a person marked in their calendar for some days. A new value is added here AND to the
 * check constraint of {@code availability.kind} (a new migration).
 */
public enum AvailabilityKind {
  /** Not available these days. */
  BUSY,
  /** Free these days: a good time to invite. */
  FREE,
  /** Away in another city; the city is said. */
  TRIP,
}
