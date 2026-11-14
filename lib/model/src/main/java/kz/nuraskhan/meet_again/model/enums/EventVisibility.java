package kz.nuraskhan.meet_again.model.enums;

/**
 * Who an event is for. A new value is added here AND to the check constraint of
 * {@code event.visibility} (a new migration).
 */
public enum EventVisibility {
  /** Only the people its creator invites, by name or by its link. */
  PRIVATE,
  /** Shown in the feed; any logged-in person can join it and enters its chat. */
  PUBLIC,
}
