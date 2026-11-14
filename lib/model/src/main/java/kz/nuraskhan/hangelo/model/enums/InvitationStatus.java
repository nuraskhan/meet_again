package kz.nuraskhan.hangelo.model.enums;

/**
 * What became of one person's place in an event. A new value is added here AND to the check
 * constraint of {@code invitation.status} (a new migration).
 */
public enum InvitationStatus {
  /** Invited by name, has not answered. */
  PENDING,
  /** Coming: said yes to an invitation, or joined by the link or from the feed. */
  ACCEPTED,
  /** Not coming by their own choice: said no, or left. May join again. */
  DECLINED,
  /** Taken out by the creator of the event. May NOT join again. */
  REMOVED,
  ;

  /** Is the person part of the event now: invited and not yet answered, or coming. */
  public boolean isIn() {
    return this == PENDING || this == ACCEPTED;
  }
}
