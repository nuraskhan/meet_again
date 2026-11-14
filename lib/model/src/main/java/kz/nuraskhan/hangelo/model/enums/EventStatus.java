package kz.nuraskhan.hangelo.model.enums;

/**
 * Where an event is in its life. A new status is added here AND to the check constraint of
 * {@code event.status} (a new migration).
 */
public enum EventStatus {
  /** Open: people can be invited, join by the link, answer. The only status things happen in. */
  ACTIVE,
  /** Called off by its creator. */
  CANCELLED,
  /** Its last proposed date has passed. Set by a timer (EventExpiryJob), never by a person. */
  EXPIRED,
}
